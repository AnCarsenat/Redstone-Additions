package ra.modules.audio

import io.github.ayfri.kore.functions.Function
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * Plays the patterns of [AudioSounds] at a block, as its marker.
 *
 * A pattern is started by [start] with its global id in `#au.id ra.temp` (the Alarm's sounds
 * from [ALARM_BASE], the Doorbell's from [DOORBELL_BASE]). The marker's [STEP] score becomes
 * `id * STEPS + tick`, and the marker is tagged [PLAYING] until the last note has played: the
 * block's per-tick look runs [step] only for a marker with that tag, so a silent block pays one
 * tag test. One score test per pattern finds the running one; no NBT is read while it plays.
 *
 * WHO HEARS IT. Every player within the block's `range`, whatever their "Machine sounds"
 * preference: an alarm or a doorbell nobody hears is not one. `volume` (percent) is the
 * playsound volume; players beyond the 16 blocks a sound carries, still inside the range, hear
 * it at a quarter of that (playsound's minimum volume), so the range is what decides who hears
 * it. The range, volume and pitch are numbers the pack writes, never text a player typed.
 */
internal object AudioPlayer {
	/** Ticks a pattern may last; the width of one pattern's block of [STEP] values. */
	const val STEPS = 64

	const val ALARM_BASE = 0
	const val DOORBELL_BASE = 16

	/** On a marker: its pattern's id * [STEPS] + the tick it is at. */
	const val STEP = "ra.au.step"

	/** On a marker while a pattern plays. */
	const val PLAYING = "ra.au.playing"

	val start = ref(Ns.AUDIO, "play/start")
	val step = ref(Ns.AUDIO, "play/step")

	private const val T = Obj.TEMP

	/** Every pattern with its global id. */
	private val patterns: List<Pair<Int, AudioSound>> =
		AudioSounds.alarm.mapIndexed { i, s -> ALARM_BASE + i to s } + AudioSounds.doorbell.mapIndexed { i, s -> DOORBELL_BASE + i to s }

	init {
		require(AudioSounds.alarm.size <= DOORBELL_BASE) { "too many alarm sounds for DOORBELL_BASE" }
	}

	fun Ra.registerAudioPlayer() {
		// As the marker, at it. #au.id ra.temp: the pattern; data.data.snd: who hears it, how loud.
		pack.define(start) {
			raw("scoreboard players set #au.w $T $STEPS")
			raw("scoreboard players operation @s $STEP = #au.id $T")
			raw("scoreboard players operation @s $STEP *= #au.w $T")
			raw("tag @s add $PLAYING")
			raw("function ${step.asId()}")
		}

		// As the marker, at it: the notes of this tick, then one tick on.
		pack.define(step) {
			for ((id, _) in patterns) {
				val base = id * STEPS
				raw("execute if score @s $STEP matches $base..${base + STEPS - 1} run return run function ${patternFn(id).asId()}")
			}
			// A step value no pattern owns: stop.
			raw("tag @s remove $PLAYING")
		}

		for ((id, sound) in patterns) {
			val base = id * STEPS
			val ticks = sound.notes.groupBy { it.at }
			pack.define(patternFn(id)) {
				for (at in ticks.keys.sorted())
					raw("execute if score @s $STEP matches ${base + at} run function ${noteFn(id, at).asId()} with entity @s data.data.snd")
				raw("scoreboard players add @s $STEP 1")
				raw("execute if score @s $STEP matches ${base + sound.length + 1}.. run tag @s remove $PLAYING")
			}
			for ((at, notes) in ticks) pack.define(noteFn(id, at)) {
				// Macro {range, vol, min}: numbers stored from scores by [storeArgs].
				for (n in notes)
					raw("\$playsound ${n.sound} block @a[distance=..\$(range)] ~ ~ ~ \$(vol) ${n.pitch} \$(min)")
			}
		}
	}

	private fun patternFn(id: Int) = ref(Ns.AUDIO, "play/p$id")
	private fun noteFn(id: Int, at: Int) = ref(Ns.AUDIO, "play/p$id/t$at")

	/**
	 * `data.data.snd` = {range, vol, min} from the block's `range` and `volume` properties. The
	 * range is capped only at [adminMax]: the players' maximum was checked when it was written,
	 * so a range an admin set stays (ra.core.Admin). As the marker.
	 */
	fun Function.storeArgs(defaultRange: Int, adminMax: Int) {
		readCappedProperty("range", default = defaultRange, min = 1, adminMax = adminMax)
		raw("execute store result entity @s data.data.snd.range int 1 run scoreboard players get #prop $T")
		readCappedProperty("volume", default = 100, min = 1, adminMax = 100)
		raw("execute store result entity @s data.data.snd.vol double 0.01 run scoreboard players get #prop $T")
		raw("execute store result entity @s data.data.snd.min double 0.0025 run scoreboard players get #prop $T")
	}

	/**
	 * `#au.id ra.temp` = [base] + `data.properties.<prop>` of @s, a wrong index read as 0. As the
	 * marker.
	 */
	fun Function.patternId(prop: String, base: Int, count: Int) {
		raw("execute store result score #au.id $T run data get entity @s data.properties.$prop")
		raw("execute unless score #au.id $T matches 0..${count - 1} run scoreboard players set #au.id $T 0")
		if (base != 0) raw("scoreboard players add #au.id $T $base")
	}

	/**
	 * `data.status.<status>` = the name of the sound at `data.properties.<prop>` in [sounds], for
	 * the Goggles. As the marker.
	 */
	fun Function.statusName(prop: String, status: String, sounds: List<AudioSound>) {
		raw("execute store result score #au.n $T run data get entity @s data.properties.$prop")
		raw("execute unless score #au.n $T matches 0..${sounds.size - 1} run scoreboard players set #au.n $T 0")
		sounds.forEachIndexed { i, s ->
			raw("execute if score #au.n $T matches $i run data modify entity @s data.status.$status set value \"${s.name}\"")
		}
	}
}
