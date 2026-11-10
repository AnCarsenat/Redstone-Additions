package ra.modules.automation.uncrafter

import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.readProperty
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw

/**
 * One look of an [Uncrafter], once a second from the automation clock. As the marker, at the block.
 *
 * INPUT ABOVE, OUTPUT BELOW. It reads the container on top of it (a hopper, chest, barrel…) and
 * puts what it makes into the container under it. Neither is the Uncrafter itself, so it never
 * finds its own output, and a hopper feeding it from above simply holds the items.
 *
 * ONE STACK A LOOK. The first slot of the input, in slot order, that it can act on:
 *  - an item with any component (damaged, enchanted, renamed, dyed, a potion, another pack's
 *    item…) or an item not in [UncraftTable]: the whole stack is passed on to the output
 *    unchanged, for free. Nothing is ever taken apart that is not exactly the vanilla item.
 *  - an item in the table, with at least as many as its recipe makes: that many are taken apart
 *    into the recipe's ingredients, for `eu_per_uncraft` EU.
 *  - a table item with fewer than its recipe makes (2 of the 3 iron doors) waits for more.
 *
 * NOTHING IS LOST. Everything is checked before anything moves: an output with enough empty slots
 * (one per ingredient, each ingredient fitting one stack), and an EU grid holding the whole price.
 * The input is taken first, then the output is written, so a failure half way can never copy
 * items.
 *
 * EU. Like the Boiler, the Uncrafter is not a network node: it reaches into the grid of a wire or
 * battery touching one of its four sides, the first one whose grid holds the whole price (all or
 * nothing: a price paid in part would leave the item whole and the grid poorer).
 */
internal object UncrafterRun {
	/** `storage ra_automation:uncraft map`: the reverse recipes, written at load. */
	const val TABLE = "ra_automation:uncraft"
	/** `storage ra_automation:uncrafter`: scratch for one look. */
	private const val S = "ra_automation:uncrafter"
	private const val T = Obj.TEMP

	/** On the neighbouring electric node that pays, for one look. */
	private const val SRC = "ra.auto.un_src"
	private val SIDES = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1")

	private fun fn(path: String) = Uncrafter.block.fn(path)

	val run = fn("run")
	private val find = fn("find")
	private val tryEntry = fn("try")
	private val pass = fn("pass")
	private val uncraft = fn("uncraft")
	private val scanEu = fn("scan_eu")
	private val take = fn("take")
	private val deliver = fn("deliver")
	private val done = fn("done")

	private fun state(text: String) = "data modify entity @s data.status.state set value \"$text\""

	fun Ra.registerUncrafterRun() {
		pack.define(run) {
			raw("execute unless block ~ ~1 ~ #ra_lib:containers run return run ${state("no container above")}")
			raw("execute unless items block ~ ~1 ~ container.* * run return run ${state("waiting for items")}")
			raw("execute if function ${RaLib.Redstone.any.asId()} run return run ${state("stopped by redstone")}")
			raw("execute unless block ~ ~-1 ~ #ra_lib:containers run return run ${state("no container below")}")

			// Empty slots below: the container's size less the stacks it holds.
			raw("execute positioned ~ ~-1 ~ run function ${RaLib.Inventory.containerSize.asId()}")
			raw("scoreboard players set #un.used $T 0")
			raw("execute store result score #un.used $T run data get block ~ ~-1 ~ Items")
			raw("scoreboard players operation #un.free $T = #inv_max $T")
			raw("scoreboard players operation #un.free $T -= #un.used $T")
			raw("execute if score #un.free $T matches ..0 run return run ${state("output full")}")

			raw("data remove storage $S d")
			raw("data modify storage $S d.scan set from block ~ ~1 ~ Items")
			raw("scoreboard players set #un.act $T 0")
			raw("function ${find.asId()}")
			raw("data remove storage $S d.scan")
			raw("execute if score #un.act $T matches 0 run ${state("waiting for a full recipe")}")
			raw("execute if score #un.act $T matches 1 run function ${pass.asId()} with storage $S d.q")
			raw("execute if score #un.act $T matches 2 run function ${uncraft.asId()} with storage $S d.q")
			raw("data remove storage $S d")
		}

		// One entry of the input, then the next, until one is picked (#un.act 1 pass, 2 uncraft).
		pack.define(find) {
			raw("execute unless score #un.act $T matches 0 run return 0")
			raw("execute unless data storage $S d.scan[0] run return 0")
			raw("data modify storage $S d.q set value {}")
			raw("data modify storage $S d.q.id set from storage $S d.scan[0].id")
			// As an int: pasted as `Slot:$(slot)b`, so it must come out as a bare number.
			raw("execute store result storage $S d.q.slot int 1 run data get storage $S d.scan[0].Slot")
			raw("data modify storage $S d.item set from storage $S d.scan[0]")
			// Any component at all: not the plain vanilla item, so passed on as it is.
			raw("execute if data storage $S d.scan[0].components run scoreboard players set #un.act $T 1")
			raw("execute if score #un.act $T matches 0 run function ${tryEntry.asId()} with storage $S d.q")
			raw("execute if score #un.act $T matches 0 run data remove storage $S d.scan[0]")
			raw("function ${find.asId()}")
		}

		// {id, slot}: the id comes from the container (a registry id, never typed by a player).
		pack.define(tryEntry) {
			raw("data remove storage $S d.hit")
			raw("\$data modify storage $S d.hit set from storage $TABLE map.\"\$(id)\"")
			raw("execute unless data storage $S d.hit run return run scoreboard players set #un.act $T 1")
			raw("\$execute store result score #un.c $T run data get block ~ ~1 ~ Items[{Slot:\$(slot)b}].count")
			raw("execute store result score #un.n $T run data get storage $S d.hit.n")
			raw("execute if score #un.c $T >= #un.n $T run scoreboard players set #un.act $T 2")
		}

		// {id, slot}: the whole stack, unchanged, to the output (one empty slot is enough for it).
		pack.define(pass) {
			raw("\$data remove block ~ ~1 ~ Items[{Slot:\$(slot)b}]")
			raw("data modify storage ra:inventory item set from storage $S d.item")
			raw("execute positioned ~ ~-1 ~ run function ${RaLib.Inventory.insertOrDrop.asId()}")
			raw("data remove storage ra:inventory item")
			raw(state("passing items on"))
		}

		// {id, slot}, with d.hit the recipe. #un.n is its count, #un.free the empty output slots.
		pack.define(uncraft) {
			raw("execute store result score #un.need $T run data get storage $S d.hit.out")
			raw("execute if score #un.free $T < #un.need $T run return run ${state("output full")}")

			readProperty("eu_per_uncraft", default = Uncrafter.DEFAULT_EU, min = 1)
			raw("scoreboard players operation #un.cost $T = #prop $T")
			raw("scoreboard players set #un.found $T 0")
			for (at in SIDES) raw("execute positioned $at as @e[type=minecraft:marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run function ${scanEu.asId()}")
			raw("execute if score #un.found $T matches 0 run return run ${state("not enough EU")}")
			raw("execute store result storage $S pay.amount int 1 run scoreboard players get #un.cost $T")
			raw("scoreboard players set #un.took $T 0")
			raw("execute as @e[type=minecraft:marker,tag=$SRC,distance=..2,limit=1] store result score #un.took $T run function ${take.asId()} with storage $S pay")
			raw("tag @e[type=minecraft:marker,tag=$SRC,distance=..2] remove $SRC")
			raw("data remove storage $S pay")
			raw("execute unless score #un.took $T = #un.cost $T run return run ${state("not enough EU")}")

			// #un.n items off the input stack.
			raw("\$execute store result score #un.c $T run data get block ~ ~1 ~ Items[{Slot:\$(slot)b}].count")
			raw("scoreboard players operation #un.c $T -= #un.n $T")
			raw("\$execute if score #un.c $T matches ..0 run data remove block ~ ~1 ~ Items[{Slot:\$(slot)b}]")
			raw("\$execute if score #un.c $T matches 1.. store result block ~ ~1 ~ Items[{Slot:\$(slot)b}].count int 1 run scoreboard players get #un.c $T")
			raw("data modify storage $S d.out set from storage $S d.hit.out")
			raw("function ${deliver.asId()}")
			raw("function ${done.asId()}")
		}

		// As a neighbouring electric node: claim it when its grid holds the whole price.
		pack.define(scanEu) {
			raw("execute if score #un.found $T matches 1 run return 0")
			raw("function ${RaLib.Transport.Net.read.asId()}")
			// The EU entry, not the total: net/take is capped by it, so a grid passing this test
			// always pays the whole price and nothing after the take can come up short.
			raw("scoreboard players set #un.have $T 0")
			raw("execute store result score #un.have $T run data get storage ra:transport cur.amounts.eu")
			raw("execute unless score #un.have $T >= #un.cost $T run return 0")
			raw("scoreboard players set #un.found $T 1")
			raw("tag @s add $SRC")
		}

		// As the chosen node. `amount` is an int this pack computed, never a player's text.
		pack.define(take) {
			raw("\$return run function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"eu\"}")
		}

		// d.out, one ingredient at a time, into the container below.
		pack.define(deliver) {
			raw("execute unless data storage $S d.out[0] run return 0")
			raw("data modify storage ra:inventory item set from storage $S d.out[0]")
			raw("execute positioned ~ ~-1 ~ run function ${RaLib.Inventory.insertOrDrop.asId()}")
			raw("data remove storage ra:inventory item")
			raw("data remove storage $S d.out[0]")
			raw("function ${deliver.asId()}")
		}

		pack.define(done) {
			raw(state("uncrafting"))
			raw("execute store result score #un.c $T run data get entity @s data.status.uncrafted")
			raw("execute store result entity @s data.status.uncrafted int 1 run scoreboard players add #un.c $T 1")
			raw("particle minecraft:crit ~ ~0.6 ~ 0.3 0.3 0.3 0.05 8 normal ${particleViewersSel()}")
			raw("playsound minecraft:block.smithing_table.use block ${soundListenersSel(12)} ~ ~ ~ 0.5 1.4")
		}
	}
}
