package ra.modules.audio

/**
 * The fixed sound lists of the Alarm and the Doorbell. A block stores only the INDEX of its sound
 * (`data.properties.sound`, a number), never a sound id: nothing a player types reaches a
 * `playsound`. Adding a sound is one line here; the index of every existing sound must stay where
 * it is, because blocks in saved worlds point at it.
 *
 * A sound is a short pattern of notes, each at a tick offset from the start ([Note.at]). Notes at
 * offset 0 play at once; later ones are played by [AudioPlayer] while the pattern runs.
 */
internal data class Note(val sound: String, val pitch: Double, val at: Int = 0) {
	init {
		require(pitch in 0.5..2.0) { "$sound: playsound pitch must be 0.5..2.0, got $pitch" }
		require(at in 0 until AudioPlayer.STEPS) { "$sound: offset $at outside 0..${AudioPlayer.STEPS - 1}" }
		require(sound.matches(Regex("minecraft:[a-z0-9_.]+"))) { "sound id '$sound'" }
	}
}

internal data class AudioSound(val name: String, val notes: List<Note>) {
	/** The last tick offset of the pattern. */
	val length get() = notes.maxOf { it.at }
}

internal object AudioSounds {
	private fun notes(sound: String, vararg pitchAt: Pair<Double, Int>) = pitchAt.map { (p, t) -> Note(sound, p, t) }

	private const val BIT = "minecraft:block.note_block.bit"
	private const val BELL_NOTE = "minecraft:block.note_block.bell"

	/** The Alarm's sounds, in index order. Each is at most 19 ticks long: it fits the 20-tick default interval. */
	val alarm = listOf(
		AudioSound("Bell", notes("minecraft:block.bell.use", 1.0 to 0)),
		AudioSound("Siren", notes(BIT, 1.19 to 0, 0.89 to 5, 1.19 to 10, 0.89 to 15)),
		AudioSound("Klaxon", notes("minecraft:block.note_block.trumpet", 0.7 to 0, 0.7 to 4, 0.7 to 8)),
		AudioSound("Beeper", notes("minecraft:block.note_block.pling", 2.0 to 0, 2.0 to 4)),
		AudioSound("Alarm clock", notes(BELL_NOTE, 1.5 to 0, 1.5 to 2, 1.5 to 4, 1.5 to 6)),
		AudioSound("Toll", notes("minecraft:block.bell.use", 0.5 to 0)),
		AudioSound("Buzzer", notes("minecraft:block.note_block.didgeridoo", 0.5 to 0, 0.5 to 2, 0.5 to 4, 0.5 to 6, 0.5 to 8)),
	)

	/** The Doorbell's chimes, in index order. */
	val doorbell = listOf(
		AudioSound("Ding-dong", notes(BELL_NOTE, 1.19 to 0, 0.94 to 8)),
		AudioSound("Westminster", notes(BELL_NOTE, 1.19 to 0, 0.94 to 6, 1.06 to 12, 0.71 to 18)),
		AudioSound("Chime", notes("minecraft:block.note_block.chime", 1.5 to 0, 1.0 to 5)),
		AudioSound("Bell", notes("minecraft:block.bell.use", 1.2 to 0)),
		AudioSound("Buzzer", notes("minecraft:block.note_block.didgeridoo", 0.8 to 0, 0.8 to 2, 0.8 to 4)),
		AudioSound("Knock", notes("minecraft:entity.zombie.attack_wooden_door", 1.6 to 0, 1.6 to 4)),
	)

	init {
		require(alarm.all { it.length < 20 }) { "an alarm sound longer than the default interval" }
	}
}
