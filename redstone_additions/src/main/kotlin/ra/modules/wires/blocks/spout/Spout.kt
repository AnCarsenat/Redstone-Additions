package ra.modules.wires.blocks.spout

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.toSnbt
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.WiresBlocks
import ra.modules.wires.WiresBlocks.stackNbt
import ra.modules.wires.common.Common
import ra.modules.wires.common.line
import ra.modules.wires.fluid.Fluid
import ra.modules.wires.machine.MachineClock

/**
 * Spout: fills one empty container a second under it from its fluid network.
 *
 * WHAT IT FILLS. The empty containers in `storage ra:wires item_fills` (ra_wires:media/init): a
 * bucket becomes a water, lava, powder snow or milk bucket (5000 mL, as a Liquid Drain empties
 * one), a glass bottle a water bottle, the network's potion (1000 mL) or a bottle o' enchanting
 * (700 mL of experience). The network's media are tried in arrival order, the primary first. A
 * potion comes out with the `potion_contents` the network has on record (its effects and colour,
 * as the first potion poured in had them).
 *
 * WHERE. In a container directly under it (hopper, chest, barrel, brewing stand...), so a
 * hopper line can feed empties in and carry the filled ones away; failing that, an empty lying
 * loose in the space under it. The filled item goes back into the same container (dropped on it
 * if full), or is dropped where the empty lay.
 *
 * ALL OR NOTHING. The network is read first; only a medium holding a whole container's worth is
 * taken, and the empty is removed after the take, with the take given back if that removal fails.
 *
 * REDSTONE LOCKS IT, as it does a hopper. Nothing under it costs one block test and one selector
 * a second.
 */
object Spout : PackPart {
	const val ID = "spout"
	const val BLOCK = "minecraft:waxed_exposed_copper_grate"

	private const val T = "ra.wires.tmp"
	private const val ITEM_TAG = "ra.wires.spout_item"
	private const val NEW_TAG = "ra.wires.spout_new"
	private const val S = "storage ra:wires sp"

	val goggles = ref(Ns.WIRES, "blocks/$ID/goggles")
	val run = ref(Ns.WIRES, "blocks/$ID/run")
	val onBreak = ref(Ns.WIRES, "blocks/$ID/on_break")
	private val potionRead = ref(Ns.WIRES, "blocks/$ID/potion_read")
	private val mediaNext = ref(Ns.WIRES, "blocks/$ID/media_next")
	private val mediaFills = ref(Ns.WIRES, "blocks/$ID/media_fills")
	private val fillNext = ref(Ns.WIRES, "blocks/$ID/fill_next")
	private val fillTry = ref(Ns.WIRES, "blocks/$ID/fill_try")
	private val sound = ref(Ns.WIRES, "blocks/$ID/sound")

	override fun Ra.register() {
		properties(ID) {}

		pack.define(Spout.run) { run() }

		// Macro {id}: the network id, from the marker's own score.
		pack.define(potionRead) {
			raw("\$execute if data storage ra:transport nets.n\$(id).potion run data modify $S.potion set from storage ra:transport nets.n\$(id).potion")
		}

		// One medium of the network after another, until a container is filled.
		pack.define(mediaNext) {
			raw("execute if score #sp.done $T matches 1 run return 0")
			raw("execute unless data $S.media[0] run return 0")
			raw("data modify $S.m set from $S.media[0]")
			raw("data remove $S.media[0]")
			function(mediaFills, Storage.WIRES, "sp.m")
			function(mediaNext)
		}

		// Macro {m}: a medium key from the network's media list.
		pack.define(mediaFills) {
			raw("\$execute unless data storage ra:wires item_fills.\$(m)[0] run return 0")
			raw("\$data modify $S.fills set from storage ra:wires item_fills.\$(m)")
			raw("\$data modify $S.medium set value \"\$(m)\"")
			function(fillNext)
		}

		pack.define(fillNext) {
			raw("execute if score #sp.done $T matches 1 run return 0")
			raw("execute unless data $S.fills[0] run return 0")
			raw("data modify $S.cur set from $S.fills[0]")
			raw("data remove $S.fills[0]")
			raw("data modify $S.cur.medium set from $S.medium")
			raw("execute if data $S.cur{potion:1b} if data $S.potion run data modify $S.cur.components.\"minecraft:potion_contents\" set from $S.potion")
			function(fillTry, Storage.WIRES, "sp.cur")
			function(fillNext)
		}

		pack.define(fillTry) { fillTry() }

		// Macro {sound}: only called when the entry names one.
		pack.define(sound) {
			raw("\$playsound \$(sound) block ${soundListenersSel(16)} ~ ~-1 ~ 0.6 1")
		}

		pack.define(Spout.goggles) {
			gogglesHeader("Spout", nameY = 1.0)
			dataLine("spout_state", "Spout: ", "yellow", y = 0.8)
			dataLine("medium", "Fluid: ", "aqua", y = 0.6)
			dataLine("amount", "Amount: ", "aqua", y = 0.4, suffix = " mL")
		}

		pack.define(Spout.onBreak) {
			// As the marker, at it. Ends by killing the marker.
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.node") }
			tag(self()) { remove(MachineClock.TAG) }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop(BLOCK)
			raw("summon item ~ ~ ~ {Item:${WiresBlocks.items.first { it.block.id == ID }.stackNbt().toSnbt()}}")
			kill(self())
		}
	}

	/** As the marker, at it, once a second from the machine clock. */
	private fun Function.run() {
		// What is under it: 1 a container holding something, 2 an item lying loose, 0 neither.
		raw("scoreboard players set #sp.src $T 0")
		raw("execute if data block ~ ~-1 ~ Items[0] run scoreboard players set #sp.src $T 1")
		raw("execute if score #sp.src $T matches 0 positioned ~ ~-1 ~ if entity @e[type=item,distance=..1,limit=1] run scoreboard players set #sp.src $T 2")
		raw("execute if score #sp.src $T matches 0 run return run data modify entity @s data.status.spout_state set value \"idle\"")

		line("execute if function", RaLib.Redstone.any, "run return run data modify entity @s data.status.spout_state set value \"locked\"")
		raw("execute unless score @s ra.tr.net matches 1.. run return run data modify entity @s data.status.spout_state set value \"no_network\"")
		function(RaLib.Transport.Net.read)
		raw("execute unless data storage ra:transport cur.media[0] run return run data modify entity @s data.status.spout_state set value \"network_empty\"")

		raw("data remove $S")
		raw("data modify $S.media set from storage ra:transport cur.media")
		raw("execute store result $S.id int 1 run scoreboard players get @s ra.tr.net")
		executeRun("if data storage ra:transport cur.amounts.potion", potionRead, "with $S")
		// #sp.why: 0 nothing under it is a container the network fills, 1 one is but the network
		// holds too little of its fluid.
		raw("scoreboard players set #sp.done $T 0")
		raw("scoreboard players set #sp.why $T 0")
		function(mediaNext)
		raw("data remove $S")

		raw("execute if score #sp.done $T matches 1 run return run data modify entity @s data.status.spout_state set value \"filling\"")
		raw("execute if score #sp.why $T matches 1 run return run data modify entity @s data.status.spout_state set value \"not_enough\"")
		raw("data modify entity @s data.status.spout_state set value \"nothing_to_fill\"")
	}

	/**
	 * Macro {empty, item, volume, medium} (an item_fills entry, `medium` added by fill_next; the
	 * whole entry is also `storage ra:wires sp.cur`). As the marker, at it.
	 */
	private fun Function.fillTry() {
		val tagged = "@e[type=item,tag=$ITEM_TAG,distance=..3]"
		// The empty, under it.
		raw("\$execute if score #sp.src $T matches 1 unless items block ~ ~-1 ~ container.* \$(empty) run return 0")
		raw("\$execute if score #sp.src $T matches 2 positioned ~ ~-1 ~ as @e[type=item,distance=..1,nbt={Item:{id:\"\$(empty)\"}},sort=nearest,limit=1] run tag @s add $ITEM_TAG")
		raw("execute if score #sp.src $T matches 2 unless entity $tagged run return 0")

		// A whole container's worth, or nothing.
		raw("scoreboard players set #sp.why $T 1")
		raw("\$scoreboard players set #sp.need $T \$(volume)")
		raw("\$execute store result score #sp.have $T run data get storage ra:transport cur.amounts.\$(medium)")
		raw("execute if score #sp.have $T < #sp.need $T run return run tag $tagged remove $ITEM_TAG")

		// Paid for first. The read just made says it is there; a short take is given back.
		raw("\$execute store result score #sp.got $T run function ${RaLib.Transport.Net.take.asId()} {amount:\$(volume),medium:\"\$(medium)\"}")
		raw("\$execute if score #sp.got $T < #sp.need $T run data modify $S.back set value {medium:\"\$(medium)\"}")
		raw("execute if score #sp.got $T < #sp.need $T store result $S.back.amount int 1 run scoreboard players get #sp.got $T")
		raw("execute if score #sp.got $T < #sp.need $T if score #sp.got $T matches 1.. run function ${RaLib.Transport.Net.offer.asId()} with $S.back")
		raw("execute if score #sp.got $T < #sp.need $T run return run tag $tagged remove $ITEM_TAG")

		// The empty comes out, CHECKED: if it did not, the fluid goes back and nothing is made.
		raw("scoreboard players set #sp.rm $T 1")
		raw("\$execute if score #sp.src $T matches 1 positioned ~ ~-1 ~ store result score #sp.rm $T run function ${RaLib.Inventory.remove.asId()} {id:\"\$(empty)\",count:1}")
		raw("\$execute if score #sp.rm $T matches ..0 run return run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(volume),medium:\"\$(medium)\"}")
		// A loose stack loses one; the last one goes.
		raw("execute if score #sp.src $T matches 2 store result score #sp.cnt $T run data get entity @e[type=item,tag=$ITEM_TAG,distance=..3,limit=1] Item.count")
		raw("execute if score #sp.src $T matches 2 run scoreboard players remove #sp.cnt $T 1")
		raw("execute if score #sp.src $T matches 2 if score #sp.cnt $T matches ..0 run kill $tagged")
		raw("execute if score #sp.src $T matches 2 if score #sp.cnt $T matches 1.. store result entity @e[type=item,tag=$ITEM_TAG,distance=..3,limit=1] Item.count int 1 run scoreboard players get #sp.cnt $T")
		raw("tag $tagged remove $ITEM_TAG")

		// The filled one, its components crossing through storage, never through a macro.
		raw("data modify storage ra:inventory item set value {count:1}")
		raw("data modify storage ra:inventory item.id set from $S.cur.item")
		raw("execute if data $S.cur.components run data modify storage ra:inventory item.components set from $S.cur.components")
		executeRun("if score #sp.src $T matches 1 positioned ~ ~-1 ~", RaLib.Inventory.insertOrDrop)
		raw("execute if score #sp.src $T matches 2 run summon item ~ ~-1 ~ {Item:{id:\"minecraft:stone\",count:1},Tags:[\"$NEW_TAG\"],Motion:[0d,0d,0d]}")
		raw("execute if score #sp.src $T matches 2 run data modify entity @e[type=item,tag=$NEW_TAG,distance=..2,limit=1] Item set from storage ra:inventory item")
		raw("tag @e[type=item,tag=$NEW_TAG,distance=..2] remove $NEW_TAG")
		raw("data remove storage ra:inventory item")

		raw("scoreboard players set #sp.done $T 1")
		raw("\$execute positioned ~ ~-1.5 ~ run function ${Fluid.drip.asId()} {medium:\"\$(medium)\"}")
		executeRun("if data $S.cur.sound", sound, "with $S.cur")
	}
}
