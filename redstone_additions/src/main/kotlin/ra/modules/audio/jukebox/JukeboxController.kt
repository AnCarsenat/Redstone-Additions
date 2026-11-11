package ra.modules.audio.jukebox

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.audio.jukebox.JukeboxOps.registerJukeboxOps
import ra.core.block.WRENCH_READOUT

/**
 * Jukebox Controller: a waxed copper bulb that runs the vanilla jukebox next to it from redstone
 * pulses, optionally playing the music discs of a chest (or barrel, or shulker box) next to it as
 * a playlist.
 *
 * PULSES. A rising edge on the bulb (`powered=true`: one block test per controller per tick) reads
 * the signal strength once and acts on it:
 *   15 (a button, lever or repeater against it): play / stop;
 *   8 to 14: the next disc;
 *   1 to 7: stop.
 *
 * DISCS. With a container next to the controller, a stopped disc goes back into it (into the slot
 * it came from when that is free) and "next" takes the next disc after that slot, round again at
 * the end, so the container's order is the playlist. Without one, "stop" keeps the disc in the
 * controller (`data.data.held`, shown on the Goggles, dropped if it is broken) and "play" puts it
 * back. A disc with nowhere to go is popped out on top of the jukebox, as vanilla does.
 *
 * CONTINUOUS (`continuous`, default on). When a disc the controller started ends, the next one
 * plays (or the same one again, without a container). Checked once a second by [JukeboxOps.poll],
 * only for controllers that are playing: an idle controller costs its block test only.
 *
 * THE BULB. Lit while the controller is playing. The bulb's own flip on a pulse is put right on
 * the same tick the pulse is handled.
 */
object JukeboxController {
	const val PHYSICAL = "minecraft:waxed_copper_bulb"
	val block = CustomBlock(Ns.AUDIO, "jukebox_controller", "Jukebox Controller", model = PHYSICAL)
	val item = block.loreItem {
		lore(
			loreLine("Plays, stops and skips the jukebox next to it", "gray"),
			loreLine("Signal 15: play/stop, 8-14: next, 1-7: stop", "dark_gray"),
			loreLine("A chest next to it is the playlist", "dark_gray"),
		)
	}

	val registerBlock = block.fn("register_block")
	private val look = block.fn("look")
	private val powered = block.fn("powered")
	private val edge = block.fn("edge")
	private val cycleContinuous = block.fn("cycle_continuous")

	/** The 27-slot containers a playlist may be kept in. */
	const val CONTAINERS = "#ra_audio:disc_containers"

	/** On the marker: its bulb was powered last tick. */
	private const val WAS = "ra.au.was_powered"

	/** On the marker: a disc it started is playing (or should be). */
	const val ON = "ra.au.on"

	private const val T = Obj.TEMP

	fun Ra.registerJukeboxController() {
		block(block)
		properties(block) {
			bool("continuous", default = true)
		}
		wrenchCycle(block.id, WrenchCycle("Continuous", "continuous", cycleContinuous))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.JUKEBOX)) {
			pattern("CKC", "RHR", "CCC")
			key("C", Items.COPPER_INGOT)
			key("K", Items.COMPARATOR)
			key("R", Items.REDSTONE)
			key("H", Items.HOPPER)
		}
		pack.blockTag("disc_containers", Ns.AUDIO.id) {
			for (b in listOf(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL)) add(b)
			add("#minecraft:shulker_boxes")
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", "{continuous:1b}")
			setOnNew(block, "data.status", """{state:"stopped",disc:"none"}""")
			setOnNew(block, "data.data", "{slot:-1}")
			finishPlacement(block)
		}

		// A rising edge, as the marker at it: the level picks the action.
		pack.define(edge) {
			raw("function ${RaLib.Redstone.detect.asId()}")
			raw("scoreboard players operation #au.lv $T = @s ${Obj.POWER}")
			raw("function ${JukeboxOps.prep.asId()}")
			raw("execute if score #au.j $T matches 0 run return run function ${JukeboxOps.fail.asId()}")
			raw("execute if score #au.lv $T matches 1..7 run function ${JukeboxOps.stop.asId()}")
			raw("execute if score #au.lv $T matches 8..14 run function ${JukeboxOps.next.asId()}")
			raw("execute unless score #au.lv $T matches 1..14 run function ${JukeboxOps.toggle.asId()}")
			raw("function ${JukeboxOps.show.asId()}")
		}
		pack.define(powered) {
			raw("execute if entity @s[tag=$WAS] run return 0")
			raw("tag @s add $WAS")
			raw("function ${edge.asId()}")
		}

		pack.define(cycleContinuous) {
			raw("execute store success score #au.c $T if data entity @s data.properties{continuous:1b}")
			raw("execute if score #au.c $T matches 1 run data modify entity @s data.properties.continuous set value 0b")
			raw("execute if score #au.c $T matches 0 run data modify entity @s data.properties.continuous set value 1b")
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"continuous ",color:"gray"},{nbt:"data.properties.continuous",entity:"@s",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			stackedStatusLine("state", "State: ", "green")
			stackedStatusLine("disc", "Disc: ", "aqua")
			stackedPropLine("continuous", "Continuous: ", "gray")
			stackedTextLine("", "Signal 15: play/stop, 8-14: next, 1-7: stop", "gray")
		}

		val onBreak = pack.blockOnBreak(
			block, drop = { dropItem(item) }, sound = "minecraft:block.copper_bulb.break", cloud = true,
			cleanup = { raw("execute if data entity @s data.data.held run function ${JukeboxOps.dropHeld.asId()}") },
		)

		// Every tick, as each marker: one block test while unpowered.
		pack.define(look) {
			raw("execute unless block ~ ~ ~ $PHYSICAL run return run function ${onBreak.asId()}")
			raw("execute if block ~ ~ ~ $PHYSICAL[powered=true] run return run function ${powered.asId()}")
			raw("tag @s[tag=$WAS] remove $WAS")
		}
		pack.define(block.tick) {
			raw("execute as ${block.markerSel()} at @s run function ${look.asId()}")
		}

		registerJukeboxOps()
	}
}
