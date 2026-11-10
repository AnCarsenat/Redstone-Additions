package ra.advancements

import io.github.ayfri.kore.generated.Items

/** Audio (`ra_audio`). */
internal val audioTree = listOf(
	category("ra_audio", Items.NOTE_BLOCK, "Audio", "Make yourself heard: alarms, doorbells and music on cue.", "get_audio_item", Items.NOTE_BLOCK),
	egg("ra_audio", "alarm", "Alarm", Items.REDSTONE_LAMP, Flag.TRUE),
	egg("ra_audio", "doorbell", "Doorbell", Items.POLISHED_BLACKSTONE_BUTTON, Flag.TRUE),
	egg("ra_audio", "jukebox_controller", "Jukebox Controller", Items.JUKEBOX, Flag.TRUE),
)
