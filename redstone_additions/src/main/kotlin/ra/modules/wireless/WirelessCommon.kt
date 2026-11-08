package ra.modules.wireless

import io.github.ayfri.kore.functions.Function
import ra.core.raw

/**
 * What the emitter and receiver share: the channel default. Both had an `enabled` property the
 * wrench toggled until v6.0.1 (the receiver lost it first, the emitter in the same release); a
 * receiver's channel and an emitter's redstone input are their only controls now.
 */

/** Channel (default "default"). As the marker. */
internal fun Function.defaultChannel() {
	raw("execute unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value \"default\"")
}
