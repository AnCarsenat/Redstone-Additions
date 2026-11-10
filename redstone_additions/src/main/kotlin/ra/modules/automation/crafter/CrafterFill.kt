package ra.modules.automation.crafter

import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw

/**
 * A [CrafterController] holding a Recipe Pattern: it fills the crafter itself.
 *
 * WHERE FROM. The source is the container on the controller's other side, opposite the crafter
 * (crafter above, chest below; crafter to the north, chest to the south). Any block of
 * `#ra_lib:containers`: a chest, barrel, hopper, dropper, a shulker box.
 *
 * ONE LOOK (every [CrafterControllerRun.PERIOD] ticks, with the rest of the controllers):
 *  1. A powered crafter is left alone.
 *  2. Every slot the Pattern names that is empty in the crafter gets ONE item of that id from the
 *     source: the first stack of it, in the source's order, with no components at all (a renamed,
 *     enchanted or damaged item is never used up as an ingredient). One per slot: the crafter
 *     uses one per slot per craft, so a look fills exactly one craft.
 *  3. Then the plain controller's look ([CrafterControllerRun.compareAndFire]): unchanged since
 *     the last look, nothing; every slot right, fire.
 *
 * NOTHING MADE, NOTHING LOST. The crafter slot is written first (`item replace ... with <id>`, a
 * plain item, the same as the one it replaces), read back, and only when it holds the item is one
 * taken off the source stack, in the same function. There is no tick in between for a block to
 * break.
 *
 * IDS. The ids come from `data.data.match`, which passed `ra_lib:safe/check` (format ID) when the
 * Pattern was loaded, so they can be pasted into `item replace` and an NBT filter. The positions
 * are the constants of [pairs].
 *
 * COST. Nothing per tick. A look with a full crafter is nine `if items` tests; a slot whose item
 * is missing from the source is one NBT filter test on the source.
 */
internal object CrafterFill {
	/** On a controller with a Recipe Pattern loaded (and [CrafterControllerRun.ARMED]). */
	const val FILL = "ra.auto.cc_fill"

	private const val CC = CrafterControllerRun.STORE
	private const val T = Obj.TEMP
	private const val EMPTY = "minecraft:air"

	private fun fn(path: String) = CrafterController.block.fn("fill/$path")

	/** As an armed filling controller, at it, with `storage d` its data. */
	val check = fn("check")
	private val at = fn("at")
	private val slots = fn("slots")
	private val one = fn("one")
	private val find = fn("find")
	private val take = fn("take")
	private val atCrafter = fn("at_crafter")

	/** Each crafter position of [CrafterControllerRun.directions], with the source across from it. */
	private val pairs = CrafterControllerRun.directions.map { d ->
		d to d.split(" ").joinToString(" ") { if (it == "~") it else if (it.startsWith("~-")) "~" + it.removePrefix("~-") else "~-" + it.removePrefix("~") }
	}

	private fun state(text: String) = "data modify entity @s data.status.state set value \"$text\""

	fun Ra.registerCrafterFill() {
		pack.define(check) {
			for ((c, s) in pairs) raw("execute if block $c minecraft:crafter run return run function ${at.asId()} {c:\"$c\",s:\"$s\"}")
			raw(state("no crafter next to it"))
		}

		// {c, s}: the crafter and the source, relative to the controller. At the controller.
		pack.define(at) {
			raw("\$execute if block \$(c) minecraft:crafter[triggered=true] run return run ${state("crafter powered")}")
			raw("data modify storage $CC f set from storage $CC d.data.match")
			raw("\$data modify storage $CC f.c set value \"\$(c)\"")
			raw("\$data modify storage $CC f.s set value \"\$(s)\"")
			raw("\$execute if block \$(s) #ra_lib:containers if items block \$(s) container.* * run function ${slots.asId()} with storage $CC f")
			raw("data remove storage $CC f")
			raw("\$execute positioned \$(c) run function ${atCrafter.asId()}")
		}

		// {c, s, s0..s8}: an empty crafter slot the Pattern names gets one, when the source has it.
		pack.define(slots) {
			for (n in 0 until 9) {
				raw("\$execute unless data storage $CC f{s$n:\"$EMPTY\"} unless items block \$(c) container.$n * if data block \$(s) Items[{id:\"\$(s$n)\"}] run function ${one.asId()} {n:$n,id:\"\$(s$n)\",c:\"\$(c)\",s:\"\$(s)\"}")
			}
		}

		// {n, id, c, s}: one item of id from the source into crafter slot n.
		pack.define(one) {
			raw("data remove storage $CC q")
			raw("\$data modify storage $CC want set value \"\$(id)\"")
			raw("\$data modify storage $CC scan set from block \$(s) Items")
			raw("function ${find.asId()}")
			raw("data remove storage $CC scan")
			raw("data remove storage $CC want")
			raw("data remove storage $CC cmp")
			raw("execute unless data storage $CC q.slot run return 0")
			raw("\$item replace block \$(c) container.\$(n) with \$(id)")
			raw("scoreboard players set #cc.put $T 0")
			raw("\$execute store success score #cc.put $T if items block \$(c) container.\$(n) \$(id)")
			raw("\$execute if score #cc.put $T matches 1 run data modify storage $CC q.s set value \"\$(s)\"")
			raw("execute if score #cc.put $T matches 1 run function ${take.asId()} with storage $CC q")
			raw("data remove storage $CC q")
		}

		// storage scan: the source's stacks; want: the id. The first stack of that id with no
		// components: its slot, as an int (pasted as `Slot:$(slot)b`). The ids are compared by
		// overwriting a copy, which fails exactly when the two strings are equal.
		pack.define(find) {
			raw("execute unless data storage $CC scan[0] run return 0")
			raw("data modify storage $CC cmp set from storage $CC want")
			raw("execute store success score #cc.diff $T run data modify storage $CC cmp set from storage $CC scan[0].id")
			raw("execute if score #cc.diff $T matches 0 unless data storage $CC scan[0].components run return run execute store result storage $CC q.slot int 1 run data get storage $CC scan[0].Slot")
			raw("data remove storage $CC scan[0]")
			raw("function ${find.asId()}")
		}

		// {s, slot}: one item off that stack of the source.
		pack.define(take) {
			raw("scoreboard players set #cc.c $T 0")
			raw("\$execute store result score #cc.c $T run data get block \$(s) Items[{Slot:\$(slot)b}].count")
			raw("scoreboard players remove #cc.c $T 1")
			raw("\$execute if score #cc.c $T matches ..0 run data remove block \$(s) Items[{Slot:\$(slot)b}]")
			raw("\$execute if score #cc.c $T matches 1.. store result block \$(s) Items[{Slot:\$(slot)b}].count int 1 run scoreboard players get #cc.c $T")
		}

		// As the controller, at the crafter.
		pack.define(atCrafter) {
			raw("execute unless items block ~ ~ ~ container.* * run data remove entity @s data.data.last")
			raw("execute unless items block ~ ~ ~ container.* * run return run ${state("waiting for the ingredients")}")
			raw("function ${CrafterControllerRun.compareAndFire.asId()}")
		}
	}
}
