package ra.modules.wireless

import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.core.block.markerSel

/**
 * `ra_wireless:channel/...`: what a channel carries, worked out only when an emitter changes.
 *
 * A channel's LEVEL is the strongest of what its emitters send: an Analog Emitter sends its input
 * level (0-15), a Wireless Emitter 15 while powered. Nothing stores it per channel -- a channel is
 * any text, and keying storage by it would mean pasting it into a path. Instead the emitter whose
 * output changed asks the others on its channel ([collect], skipping itself), so the level before
 * and after the change are both known:
 *
 *   other = max over the other emitters on the channel
 *   before = max(other, old)    after = max(other, new)
 *
 * Every Analog Receiver on the channel then gets `after` ([push]: written only when it differs
 * from what it shows), and the channel TURNED ON when other = 0, old = 0 and new > 0, which is
 * when Pagers ring ([page], see tools/Pager.kt).
 *
 * COST. Nothing per tick. An emitter calls [changed] on the tick its own output changes; an
 * Analog Receiver rebuilds its level from scratch only when it is placed and every
 * [AnalogReceiver.RESYNC] ticks (which picks up a channel edited in the Data Handler).
 *
 * Inputs to [changed], as the emitter: `#wl.old ra.temp` and `#wl.new ra.temp`, what it sent
 * before and sends now.
 */
internal object Channel {
	private fun fn(path: String) = ref(Ns.WIRELESS, "channel/$path")

	/** The level an Analog Emitter sends / an Analog Receiver shows, on its marker. */
	const val LEVEL = "ra.wl.level"

	/** On the emitter running [changed], so [collect] leaves it out. */
	const val SELF = "ra.wl.self"

	val changed = fn("changed")
	val collect = fn("collect")
	val matches = fn("matches")
	private val scanAnalog = fn("scan_analog")
	private val scanDigital = fn("scan_digital")

	/** As an Analog Receiver, at it: show `#wl.level` unless it already does. */
	val push = fn("push")

	/** As the emitter that turned its channel on: ring the Pagers on it. Defined by the Pager. */
	val page = fn("page")

	private const val T = Obj.TEMP

	fun Ra.registerChannel() {
		// As a block on a channel: 1 when data.properties.channel equals storage ra:temp
		// wl_channel. Compared as values, never pasted: overwriting one with the other changes
		// nothing exactly when they are equal. The copy is removed first so a block without a
		// channel cannot inherit the previous block's.
		pack.define(matches) {
			raw("data remove storage ra:temp wl_check")
			raw("data modify storage ra:temp wl_check set from entity @s data.properties.channel")
			raw("execute store success score #wl.diff $T run data modify storage ra:temp wl_check set from storage ra:temp wl_channel")
			raw("execute if score #wl.diff $T matches 0 run return 1")
			raw("return 0")
		}

		// The strongest level the emitters on storage ra:temp wl_channel send, into #wl.other.
		// Skips any marker tagged [SELF]. A Wireless Emitter counts as 15, so once that is reached
		// they are not asked at all.
		pack.define(collect) {
			executeRun("as ${AnalogEmitter.block.markerSel("tag=!$SELF", "scores={$LEVEL=1..}")}", scanAnalog)
			executeRun("if score #wl.other $T matches ..14 as ${Emitter.block.markerSel("tag=${Emitter.TRANSMITTING}", "tag=!$SELF")}", scanDigital)
		}
		pack.define(scanAnalog) {
			raw("execute unless function ${matches.asId()} run return 0")
			raw("scoreboard players operation #wl.other $T > @s $LEVEL")
		}
		pack.define(scanDigital) {
			raw("execute if function ${matches.asId()} run scoreboard players set #wl.other $T 15")
		}

		pack.define(changed) {
			raw("data modify storage ra:temp wl_channel set from entity @s data.properties.channel")
			raw("tag @s add $SELF")
			raw("scoreboard players set #wl.other $T 0")
			raw("function ${collect.asId()}")
			raw("tag @s remove $SELF")

			raw("scoreboard players operation #wl.level $T = #wl.other $T")
			raw("scoreboard players operation #wl.level $T > #wl.new $T")
			raw("execute as ${AnalogReceiver.block.markerSel()} at @s if function ${matches.asId()} run function ${push.asId()}")

			// Off before, on now: the channel turned on.
			raw("execute if score #wl.other $T matches 0 if score #wl.old $T matches 0 if score #wl.new $T matches 1.. run function ${page.asId()}")
		}
	}
}
