package ra.modules.audio

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Admin.readCappedProperty
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
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.audio.AudioPlayer.patternId
import ra.modules.audio.AudioPlayer.statusName
import ra.modules.audio.AudioPlayer.storeArgs
import ra.core.block.WRENCH_READOUT

/**
 * Alarm: a redstone lamp that, while powered, plays its `sound` every `interval` ticks, heard by
 * every player within `range`.
 *
 * POWER. The lamp lights while it is powered, so `redstone_lamp[lit=true]` is the input: one block
 * test per alarm per tick, no redstone read. A lamp stays lit 4 ticks after its power goes, as in
 * vanilla, so the alarm may ring once more in that time.
 *
 * TIMING. `ra.cooldown` on the marker is the game time of the next ring. A ring sets it to now +
 * `interval`; an alarm switched on after a silence longer than that rings at once. A ring due
 * while the last sound still plays waits for it to end, so sounds never overlap.
 *
 * SOUNDS. `sound` is an index into [AudioSounds.alarm]; the Goggles show its name. Wrench,
 * Shift+RMB steps to the next one and plays it once.
 */
object Alarm {
	val block = CustomBlock(Ns.AUDIO, "alarm", "Alarm", model = "minecraft:redstone_lamp")
	val item = block.loreItem {
		lore(
			loreLine("Sounds an alarm while powered", "gray"),
			loreLine("Wrench, Shift+RMB: next sound", "dark_gray"),
		)
	}

	val registerBlock = block.fn("register_block")
	private val look = block.fn("look")
	private val ring = block.fn("ring")
	private val cycleSound = block.fn("cycle_sound")

	const val DEFAULT_RANGE = 32
	const val MAX_RANGE = 64
	/** An admin's ceiling (ra.core.Admin): a sound is one selector per ring, not a tick cost. */
	const val ADMIN_MAX_RANGE = 256
	const val DEFAULT_INTERVAL = 20
	const val MIN_INTERVAL = 10
	const val MAX_INTERVAL = 1200

	private const val T = Obj.TEMP
	private const val NOW = "#au.now"
	private val sounds = AudioSounds.alarm

	fun Ra.registerAlarm() {
		block(block)
		properties(block) {
			number("sound", min = 0, max = sounds.size - 1, default = 0, goggles = false)
			number("interval", min = MIN_INTERVAL, max = MAX_INTERVAL, default = DEFAULT_INTERVAL)
			number("range", min = 1, max = MAX_RANGE, adminMax = ADMIN_MAX_RANGE, default = DEFAULT_RANGE)
			number("volume", min = 10, max = 100, default = 100)
		}
		wrenchCycle(block.id, WrenchCycle("Sound", "sound", cycleSound))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.REDSTONE_LAMP)) {
			pattern("INI", "RLR", "III")
			key("I", Items.IRON_INGOT)
			key("N", Items.NOTE_BLOCK)
			key("R", Items.REDSTONE)
			key("L", Items.REDSTONE_LAMP)
		}

		// As the bat, at the block.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", "{sound:0,interval:$DEFAULT_INTERVAL,range:$DEFAULT_RANGE,volume:100}")
			setOnNew(block, "data.status", """{sound:"${sounds[0].name}",state:"silent"}""")
			raw("scoreboard players set ${block.newMarkerSel()} ${Obj.COOLDOWN} 0")
			finishPlacement(block)
		}

		// As the marker, at it, with #au.now set.
		pack.define(ring) {
			storeArgs(DEFAULT_RANGE, ADMIN_MAX_RANGE)
			readCappedProperty("interval", default = DEFAULT_INTERVAL, min = MIN_INTERVAL, adminMax = MAX_INTERVAL)
			raw("scoreboard players operation @s ${Obj.COOLDOWN} = $NOW $T")
			raw("scoreboard players operation @s ${Obj.COOLDOWN} += #prop $T")
			patternId("sound", AudioPlayer.ALARM_BASE, sounds.size)
			raw("function ${AudioPlayer.start.asId()}")
		}

		// Wrench, Shift+RMB: the next sound, played once. As the marker, at it.
		pack.define(cycleSound) {
			raw("execute store result score #au.c $T run data get entity @s data.properties.sound")
			raw("scoreboard players add #au.c $T 1")
			raw("execute unless score #au.c $T matches 0..${sounds.size - 1} run scoreboard players set #au.c $T 0")
			raw("execute store result entity @s data.properties.sound int 1 run scoreboard players get #au.c $T")
			statusName("sound", "sound", sounds)
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{nbt:"data.status.sound",entity:"@s",color:"aqua"}]""")
			storeArgs(DEFAULT_RANGE, ADMIN_MAX_RANGE)
			patternId("sound", AudioPlayer.ALARM_BASE, sounds.size)
			raw("function ${AudioPlayer.start.asId()}")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			statusName("sound", "sound", sounds)
			raw("""execute if block ~ ~ ~ minecraft:redstone_lamp[lit=true] run data modify entity @s data.status.state set value "sounding"""")
			raw("""execute unless block ~ ~ ~ minecraft:redstone_lamp[lit=true] run data modify entity @s data.status.state set value "silent"""")
			stackedStatusLine("sound", "Sound: ", "aqua")
			stackedStatusLine("state", "State: ", "green")
			stackedPropLine("interval", "Every: ", "gray", suffix = "t")
			stackedPropLine("range", "Range: ", "gray")
			stackedPropLine("volume", "Volume: ", "gray", suffix = "%")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.glass.break", cloud = true)

		// Every tick, as each marker: one block test when idle; the ring when lit and due; the
		// notes of a pattern that is playing.
		pack.define(look) {
			raw("execute unless block ~ ~ ~ minecraft:redstone_lamp run return run function ${onBreak.asId()}")
			raw("execute if entity @s[tag=${AudioPlayer.PLAYING}] run function ${AudioPlayer.step.asId()}")
			// A sound still playing is never cut off: a ring waits for it (an interval shorter than the
			// sound acts as the sound's own length).
			raw("execute if block ~ ~ ~ minecraft:redstone_lamp[lit=true] unless entity @s[tag=${AudioPlayer.PLAYING}] unless score @s ${Obj.COOLDOWN} > $NOW $T run function ${ring.asId()}")
		}
		pack.define(block.tick) {
			raw("execute store result score $NOW $T run time query gametime")
			raw("execute as ${block.markerSel()} at @s run function ${look.asId()}")
		}
	}
}
