package ra.settings.pages

import ra.core.Ns
import ra.modules.audio.Alarm
import ra.modules.audio.Doorbell

val AudioPage = Page(
	"audio", "Audio", Ns.AUDIO,
	listOf(
		PropRow("alarm", "interval", "Alarm interval (ticks)", default = Alarm.DEFAULT_INTERVAL, min = Alarm.MIN_INTERVAL, max = Alarm.MAX_INTERVAL, step = 10),
		// Ranges go to the admin ceiling (ra.core.Admin), as the display scales do.
		PropRow("alarm", "range", "Alarm range", default = Alarm.DEFAULT_RANGE, min = 1, max = Alarm.ADMIN_MAX_RANGE, step = 8),
		PropRow("doorbell", "range", "Doorbell range", default = Doorbell.DEFAULT_RANGE, min = 1, max = Doorbell.ADMIN_MAX_RANGE, step = 8),
	) + blocks(
		"alarm" to "Alarm",
		"doorbell" to "Doorbell",
		"jukebox_controller" to "Jukebox Controller",
	),
)
