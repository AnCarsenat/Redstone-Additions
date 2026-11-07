package ra.modules.storage

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.ModulesApi
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.define
import ra.core.raw
import ra.modules.infinite.breakCheck
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.modules.infinite.registerBlockMessage
import ra.modules.interactive.CHEST_HALVES
import ra.modules.interactive.breakCloud
import ra.modules.interactive.breakSound
import ra.modules.interactive.dropSpec
import ra.modules.interactive.executeRun
import ra.modules.interactive.has
import ra.modules.interactive.shapedRecipe
import ra.modules.interactive.specGive
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.scoreboard.scoreboard

/**
 * Unboxer: while powered, empties one Item Crate from `input1` into `output1`, completely.
 *
 * A BARREL wearing a dispenser skin, not a dispenser. Its input1 is `~ ~ ~` -- it reads the crates
 * in its own inventory -- and a vanilla dispenser fires its own contents on any rising redstone
 * edge. That is what threw crates on the floor mid-unbox. A barrel has the same 27-slot inventory
 * and GUI and cannot dispense at all.
 */
object Unboxer {
	val block = CustomBlock(Ns.STORAGE, "unboxer", "Unboxer", model = "minecraft:dispenser", physical = "minecraft:barrel", dirType = 2)
	val item = block.loreItem { lore(loreLine("Put crates inside; unboxes them into the block in front", "gray")) }
	val registerBlock = block.fn("register_block")
	private val process = block.fn("process")
	private val partner = block.fn("partner")
	private val refreshDisplay = block.fn("refresh_display")

	fun Ra.registerUnboxer() {
		block(block)
		pack.registerBlockMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(
			block.ns.id, block.id, item,
			pattern = listOf("III", "RDR", "PPP"),
			keys = listOf("I" to listOf(Items.IRON_INGOT), "R" to listOf(Items.REDSTONE), "D" to listOf(Items.DISPENSER), "P" to listOf(Items.PAPER)),
			unlock = has(Items.DISPENSER),
		)

		pack.define(block.handlePlacement) {
			// Lay the dispenser skin over the barrel once the IO is seeded.
			handlePlacement(block, input = "~ ~ ~") { function(refreshDisplay) }
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(process) { process() }
		pack.define(partner) { partner() }
		pack.define(refreshDisplay) {
			// The block underneath is a barrel, because the Unboxer holds the crates it is unboxing in
			// its own inventory and a vanilla dispenser fires its own contents on any rising redstone
			// edge. See ra_lib:skin/apply for the full reasoning.
			function(RaLib.Skin.apply, nbt { this["real"] = block.physical; this["skin"] = block.model; this["id"] = block.id })
		}

		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropSpec(item)
			breakSound()
			breakCloud()
			function(RaLib.Skin.clear, nbt { this["id"] = block.id })
			kill(self())
		}

		pack.define(block.tick) {
			breakCheck(block, Blocks.BARREL, onBreak)

			// Matches handle_placement: the Unboxer reads the crates in its own inventory.
			ioDefaults(block, input = "~ ~ ~")

			// Rebuild a skin that went missing (chunk reload, /kill, a rotation). Only the Unboxers
			// actually lacking one do any work.
			//
			// Anchored to the block centre, matching where ra_lib:skin/spawn stands its displays. The
			// old wide radius could not tell our skin from the neighbour's, so an Unboxer whose skin
			// was missing saw the Unboxer next door's and decided it had nothing to repair -- a pair
			// of adjacent Unboxers could settle with one skin between them and never fix it. This
			// also drives the migration off the old corner anchor: a pre-centre skin is 0.866 away,
			// fails this test, and the refresh it triggers replaces it.
			executeRun("as @e[type=marker,tag=${block.tag}] at @s align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.unboxer,distance=..0.4,limit=1]", refreshDisplay)

			executeRun("as @e[type=marker,tag=${block.tag}] at @s", RaLib.Redstone.detectSwitch)
			// Powered to run, as originally designed. The redstone lock introduced earlier only
			// existed to avoid arming the vanilla dispenser trigger; a barrel has no such trigger, so
			// there is no reason to invert the control.
			executeRun("as @e[type=marker,tag=${block.tag},tag=${Tag.POWERED}] at @s", process, "with entity @s data.properties")
		}
	}

	/**
	 * Macro `{input1, output1}`: empty one crate from input1 into output1, completely.
	 *
	 * The crate is emptied in a single activation. Moving one stored stack per cooldown left a
	 * partly-full crate sitting in the input between cycles, which is what made it look like only
	 * the first couple of stacks came out.
	 *
	 * Nothing is destroyed: each stack goes out through insert_or_drop, so whatever the output
	 * container cannot hold is dropped as an item entity instead.
	 */
	private fun Function.process() {
		// Hopper-like cooldown.
		raw("execute unless score @s ${Obj.COOLDOWN} matches -2147483648.. run scoreboard players set @s ${Obj.COOLDOWN} 0")
		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute if score @s ${Obj.COOLDOWN} matches ..3 run return 0")

		raw("\$execute positioned \$(input1) unless block ~ ~ ~ #ra_lib:containers run return 0")
		raw("\$execute positioned \$(output1) unless block ~ ~ ~ #ra_lib:containers run return 0")

		// One candidate crate from input1 (or the partner chest half).
		raw("\$execute positioned \$(input1) run function ${ModulesApi.Interactive.itemMoverSelectInput.asId()}")
		raw("execute if score #mover_has_input ${Obj.TEMP} matches 0 run return 0")

		// Only crates (new key or legacy key). An already-empty crate is accepted too, so it gets
		// flushed to the output instead of blocking the input.
		raw("execute unless data storage ra:temp mover_item.components.\"minecraft:custom_data\".ra.item_box unless data storage ra:temp mover_item.components.\"minecraft:custom_data\".ra.storage_box_item run return 0")

		// The loop runs positioned at whichever container half holds the crate, so the output
		// offset has to travel with it -- `at @s` inside resets to the marker before applying it,
		// since caret offsets are relative to the current position.
		raw("\$data modify storage ra:temp unboxer.output set value \"\$(output1)\"")
		scoreboard.players.set(literal("#unboxer_moved"), Obj.TEMP, 0)

		raw("\$execute positioned \$(input1) run execute if score #mover_input_partner ${Obj.TEMP} matches 0 run function ${StorageBox.emptyCrateHere.asId()}")
		raw("\$execute positioned \$(input1) if score #mover_input_partner ${Obj.TEMP} matches 1 run function ${partner.asId()}")

		raw("data remove storage ra:temp unboxer")
		raw("execute if score #unboxer_moved ${Obj.TEMP} matches 0 run return 0")

		scoreboard.players.set(self(), Obj.COOLDOWN, 0)
		raw("playsound minecraft:block.wood.break block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.8 1.1")
		raw("particle minecraft:cloud ~ ~0.2 ~ 0.2 0.2 0.2 0.01 5 normal @a[scores={${Obj.USER_PARTICLES}=1..}]")
	}

	/**
	 * The far half of a double chest. Already positioned at the Unboxer's input block.
	 *
	 * WHY THESE SIXTEEN LINES LIVE IN THEIR OWN FUNCTION. They used to sit inline in process, each
	 * one a macro line reading
	 * `$execute positioned $(input1) run execute if score #mover_input_partner ... matches 1 ...`.
	 * A guard written that way is not a guard: all sixteen commands still run, all sixteen still
	 * instantiate a macro, and all sixteen still resolve $(input1), every pass, even when the input
	 * is an ordinary single chest and none of them can ever match. Hoisting the condition and the
	 * positioning to the one call site turns sixteen wasted macro expansions into one comparison.
	 */
	private fun Function.partner() {
		for (h in CHEST_HALVES) executeRun(h.toPartner, StorageBox.emptyCrateHere)
	}
}
