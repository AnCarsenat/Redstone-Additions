package ra.modules.audio.jukebox

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.markerSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.audio.jukebox.JukeboxController.CONTAINERS
import ra.modules.audio.jukebox.JukeboxController.ON
import ra.modules.audio.jukebox.JukeboxController.PHYSICAL
import ra.modules.audio.jukebox.JukeboxController.block

/**
 * What the Jukebox Controller does to the jukebox and the container next to it. Everything here
 * runs as the controller's marker, at it, on a pulse or on the once-a-second check of a playing
 * controller, never on an idle tick.
 *
 * WHERE. [prep] finds the jukebox and the container among the six neighbours (first found wins)
 * and leaves their offsets in `storage ra:temp au` ({jx,jy,jz} and {cx,cy,cz}), which the macro
 * lines paste as `~<offset>`: integers the pack wrote, never text. `#au.j` / `#au.c` say whether
 * each was found; `#au.has` whether the jukebox holds a disc, `#au.play` whether it is playing
 * (its block entity carries `ticks_since_song_started` only while a song plays).
 *
 * MOVING A DISC. `item replace` is the only way a jukebox starts or stops a song: it sets the
 * jukebox's item as a hopper would, which plays it (or, with air, stops it). `data modify` on
 * `RecordItem` changes the item silently, so it is never used to play. A disc kept in the
 * controller is put back through a temporary item display's `contents` slot.
 *
 * SLOTS. `data.data.slot` is the container slot the playing disc came from (-1 none): a stopped
 * disc goes back there when it is free, and "next" looks from the slot after it.
 */
internal object JukeboxOps {
	val prep = fn("prep")
	val toggle = fn("toggle")
	val next = fn("next")
	val stop = fn("stop")
	val show = fn("show")
	val fail = fn("fail")
	val dropHeld = fn("drop_held")

	private val probe = fn("probe")
	private val play = fn("play")
	private val restart = fn("restart")
	private val fromHeld = fn("from_held")
	private val fromChest = fn("from_chest")
	private val take = fn("take")
	private val toChest = fn("to_chest")
	private val put = fn("put")
	private val toHeld = fn("to_held")
	private val eject = fn("eject")
	private val flushHeld = fn("flush_held")
	private val heldToSlot = fn("held_to_slot")
	private val findNextAt = fn("find_next_at")
	private val findNext = fn("find_next")
	private val findEmptyAt = fn("find_empty_at")
	private val findEmpty = fn("find_empty")
	private val showDisc = fn("show_disc")
	private val pollOne = fn("poll_one")
	private val ended = fn("ended")

	private val pollRun = ref(Ns.AUDIO, "poll/run")
	private val pollStart = ref(Ns.AUDIO, "poll/start")

	private fun fn(sub: String) = block.fn(sub)

	private const val T = Obj.TEMP
	private const val ARGS = "storage ra:temp au"
	private const val J = "~\$(jx) ~\$(jy) ~\$(jz)"
	private const val C = "~\$(cx) ~\$(cy) ~\$(cz)"
	private const val TMP = "ra.au.tmp"
	private const val POP = "ra.au.pop"
	private const val DISC = "*[minecraft:jukebox_playable]"
	private const val SLOTS = 27
	private const val POLL_FLAG = "#au.poll"
	private const val POLL_PERIOD = 20

	/** The six neighbours: the offset text and its {x,y,z}. */
	private val sides = listOf(
		Triple(1, 0, 0), Triple(-1, 0, 0), Triple(0, 1, 0), Triple(0, -1, 0), Triple(0, 0, 1), Triple(0, 0, -1),
	)

	/**
	 * In the module's load: the flag is reset, and the check started again for any playing
	 * controller loaded now (`replace` makes a second start harmless). Otherwise it is started by
	 * [show], the moment a controller starts playing: nothing runs per tick for it.
	 */
	fun Function.resetPoll() {
		raw("scoreboard players set $POLL_FLAG $T 0")
		raw("execute if entity ${block.markerSel("tag=$ON", "limit=1")} run function ${pollStart.asId()}")
	}

	fun Ra.registerJukeboxOps() {
		pack.define(prep) {
			raw("data modify storage ra:temp au set value {}")
			raw("scoreboard players set #au.j $T 0")
			raw("scoreboard players set #au.c $T 0")
			raw("scoreboard players set #au.has $T 0")
			raw("scoreboard players set #au.play $T 0")
			for ((x, y, z) in sides) {
				val at = "~$x ~$y ~$z"
				raw("execute if score #au.j $T matches 0 if block $at minecraft:jukebox run data modify storage ra:temp au merge value {jx:$x,jy:$y,jz:$z}")
				raw("execute if score #au.j $T matches 0 if block $at minecraft:jukebox run scoreboard players set #au.j $T 1")
			}
			for ((x, y, z) in sides) {
				val at = "~$x ~$y ~$z"
				raw("execute if score #au.c $T matches 0 if block $at $CONTAINERS run data modify storage ra:temp au merge value {cx:$x,cy:$y,cz:$z}")
				raw("execute if score #au.c $T matches 0 if block $at $CONTAINERS run scoreboard players set #au.c $T 1")
			}
			raw("execute store result score #au.s $T run data get entity @s data.data.slot")
			raw("execute unless data entity @s data.data.slot run scoreboard players set #au.s $T -1")
			raw("execute if score #au.j $T matches 1 run function ${probe.asId()} with $ARGS")
			// A disc kept while there was no container goes into one that is there now.
			raw("execute if score #au.c $T matches 1 if data entity @s data.data.held run function ${flushHeld.asId()}")
		}
		pack.define(probe) {
			raw("scoreboard players set #au.has $T 0")
			raw("scoreboard players set #au.play $T 0")
			raw("\$execute if items block $J container.0 * run scoreboard players set #au.has $T 1")
			raw("\$execute if data block $J ticks_since_song_started run scoreboard players set #au.play $T 1")
		}

		pack.define(toggle) {
			raw("execute if score #au.play $T matches 1 run return run function ${stop.asId()}")
			raw("function ${play.asId()}")
		}
		// Play: the disc in the jukebox again, else the kept one, else the next from the container.
		pack.define(play) {
			raw("execute if score #au.has $T matches 1 run return run function ${restart.asId()} with $ARGS")
			raw("execute if data entity @s data.data.held run return run function ${fromHeld.asId()} with $ARGS")
			raw("execute if score #au.c $T matches 1 run return run function ${fromChest.asId()}")
			raw("function ${fail.asId()}")
		}
		// Next: the disc back into the container, then the one after it. Without a container, play.
		pack.define(next) {
			raw("execute if score #au.c $T matches 0 run return run function ${play.asId()}")
			raw("scoreboard players set #au.done $T 0")
			raw("execute if score #au.has $T matches 1 run function ${toChest.asId()}")
			// A full container keeps the disc where it is: it plays again.
			raw("execute if score #au.has $T matches 1 if score #au.done $T matches 0 run return run function ${restart.asId()} with $ARGS")
			raw("function ${fromChest.asId()}")
		}
		pack.define(stop) {
			raw("tag @s remove $ON")
			raw("execute if score #au.has $T matches 0 run return 0")
			raw("scoreboard players set #au.done $T 0")
			raw("execute if score #au.c $T matches 1 run function ${toChest.asId()}")
			raw("execute if score #au.done $T matches 0 unless data entity @s data.data.held run function ${toHeld.asId()} with $ARGS")
			raw("execute if score #au.done $T matches 0 run function ${eject.asId()} with $ARGS")
		}

		pack.define(restart) {
			raw("\$item replace block $J container.0 from block $J container.0")
			raw("tag @s add $ON")
		}
		pack.define(fromHeld) {
			raw("tag @s remove $ON")
			raw("""summon minecraft:item_display ~ ~ ~ {Tags:["ra","$TMP"],item:{id:"minecraft:stone",count:1}}""")
			raw("data modify entity @n[type=minecraft:item_display,tag=$TMP] item set from entity @s data.data.held")
			raw("\$item replace block $J container.0 from entity @n[type=minecraft:item_display,tag=$TMP] contents")
			raw("kill @e[type=minecraft:item_display,tag=$TMP]")
			// Forgotten only once it is in: a failed copy keeps it in the controller.
			raw("\$execute if items block $J container.0 * run tag @s add $ON")
			raw("execute if entity @s[tag=$ON] run data remove entity @s data.data.held")
		}
		pack.define(fromChest) {
			raw("function ${findNextAt.asId()} with $ARGS")
			raw("execute if score #au.n $T matches -1 run return run function ${fail.asId()}")
			raw("execute store result storage ra:temp au.n int 1 run scoreboard players get #au.n $T")
			raw("execute store result entity @s data.data.slot int 1 run scoreboard players get #au.n $T")
			raw("scoreboard players operation #au.s $T = #au.n $T")
			raw("function ${take.asId()} with $ARGS")
			raw("tag @s add $ON")
		}
		pack.define(take) {
			raw("\$item replace block $J container.0 from block $C container.\$(n)")
			raw("\$item replace block $C container.\$(n) with air")
		}
		// Sets #au.done 1 when the disc went in.
		pack.define(toChest) {
			raw("function ${findEmptyAt.asId()} with $ARGS")
			raw("execute if score #au.e $T matches -1 run return 0")
			raw("execute store result storage ra:temp au.n int 1 run scoreboard players get #au.e $T")
			raw("execute store result entity @s data.data.slot int 1 run scoreboard players get #au.e $T")
			raw("scoreboard players operation #au.s $T = #au.e $T")
			raw("function ${put.asId()} with $ARGS")
			raw("scoreboard players set #au.done $T 1")
		}
		pack.define(put) {
			raw("\$item replace block $C container.\$(n) from block $J container.0")
			raw("\$item replace block $J container.0 with air")
		}
		pack.define(toHeld) {
			raw("\$data modify entity @s data.data.held set from block $J RecordItem")
			raw("\$item replace block $J container.0 with air")
			raw("scoreboard players set #au.done $T 1")
		}
		pack.define(eject) {
			raw("""${'$'}execute positioned $J run summon minecraft:item ~ ~1 ~ {Tags:["ra","$POP"],Item:{id:"minecraft:stone",count:1}}""")
			raw("\$data modify entity @n[type=minecraft:item,tag=$POP] Item set from block $J RecordItem")
			raw("tag @e[type=minecraft:item,tag=$POP] remove $POP")
			raw("\$item replace block $J container.0 with air")
		}
		pack.define(flushHeld) {
			raw("function ${findEmptyAt.asId()} with $ARGS")
			raw("execute if score #au.e $T matches -1 run return 0")
			raw("execute store result storage ra:temp au.n int 1 run scoreboard players get #au.e $T")
			raw("function ${heldToSlot.asId()} with $ARGS")
		}
		pack.define(heldToSlot) {
			raw("""summon minecraft:item_display ~ ~ ~ {Tags:["ra","$TMP"],item:{id:"minecraft:stone",count:1}}""")
			raw("data modify entity @n[type=minecraft:item_display,tag=$TMP] item set from entity @s data.data.held")
			raw("\$item replace block $C container.\$(n) from entity @n[type=minecraft:item_display,tag=$TMP] contents")
			raw("kill @e[type=minecraft:item_display,tag=$TMP]")
			raw("\$execute if items block $C container.\$(n) * run data remove entity @s data.data.held")
		}
		pack.define(dropHeld) {
			raw("""summon minecraft:item ~ ~ ~ {Tags:["ra","$POP"],Item:{id:"minecraft:stone",count:1}}""")
			raw("data modify entity @n[type=minecraft:item,tag=$POP] Item set from entity @s data.data.held")
			raw("tag @e[type=minecraft:item,tag=$POP] remove $POP")
			raw("data remove entity @s data.data.held")
		}

		// Positioned at the container. #au.n: the first disc slot after #au.s, round again; -1 none.
		pack.define(findNextAt) { raw("\$execute positioned $C run function ${findNext.asId()}") }
		pack.define(findNext) {
			raw("scoreboard players set #au.n $T -1")
			for (n in 0 until SLOTS)
				raw("execute if score #au.n $T matches -1 if score #au.s $T matches ..${n - 1} if items block ~ ~ ~ container.$n $DISC run scoreboard players set #au.n $T $n")
			for (n in 0 until SLOTS)
				raw("execute if score #au.n $T matches -1 if items block ~ ~ ~ container.$n $DISC run scoreboard players set #au.n $T $n")
		}
		// Positioned at the container. #au.e: slot #au.s when it is empty, else the first empty one; -1 full.
		pack.define(findEmptyAt) { raw("\$execute positioned $C run function ${findEmpty.asId()}") }
		pack.define(findEmpty) {
			raw("scoreboard players set #au.e $T -1")
			for (n in 0 until SLOTS)
				raw("execute if score #au.s $T matches $n unless items block ~ ~ ~ container.$n * run scoreboard players set #au.e $T $n")
			for (n in 0 until SLOTS)
				raw("execute if score #au.e $T matches -1 unless items block ~ ~ ~ container.$n * run scoreboard players set #au.e $T $n")
		}

		// After an action or a check: the status, and the bulb lit while playing.
		pack.define(show) {
			raw("execute if score #au.j $T matches 1 run function ${probe.asId()} with $ARGS")
			raw("execute if score #au.j $T matches 0 run tag @s remove $ON")
			raw("execute if score #au.j $T matches 1 if score #au.has $T matches 0 run tag @s remove $ON")
			raw("""data modify entity @s data.status.state set value "stopped"""")
			raw("""execute if entity @s[tag=$ON] run data modify entity @s data.status.state set value "playing"""")
			raw("execute if entity @s[tag=$ON] unless score $POLL_FLAG $T matches 1 run function ${pollStart.asId()}")
			raw("""execute if score #au.j $T matches 0 run data modify entity @s data.status.state set value "no jukebox"""")
			raw("""data modify entity @s data.status.disc set value "none"""")
			raw("""execute if data entity @s data.data.held run data modify entity @s data.status.disc set from entity @s data.data.held.id""")
			raw("execute if score #au.has $T matches 1 run function ${showDisc.asId()} with $ARGS")
			for (p in listOf("true", "false")) {
				raw("execute if entity @s[tag=$ON] if block ~ ~ ~ $PHYSICAL[lit=false,powered=$p] run setblock ~ ~ ~ $PHYSICAL[lit=true,powered=$p] replace")
				raw("execute if entity @s[tag=!$ON] if block ~ ~ ~ $PHYSICAL[lit=true,powered=$p] run setblock ~ ~ ~ $PHYSICAL[lit=false,powered=$p] replace")
			}
		}
		pack.define(showDisc) { raw("\$data modify entity @s data.status.disc set from block $J RecordItem.id") }

		pack.define(fail) {
			raw("playsound minecraft:block.note_block.bass block ${soundListenersSel(16)} ~ ~ ~ 0.6 0.7")
			raw("function ${show.asId()}")
		}

		// Once a second, while some controller is playing: a song that ended plays the next one.
		pack.define(pollStart) {
			raw("scoreboard players set $POLL_FLAG $T 1")
			schedule(pollRun).replace(POLL_PERIOD.ticks)
		}
		pack.define(pollRun) {
			raw("scoreboard players set $POLL_FLAG $T 0")
			raw("execute as ${block.markerSel("tag=$ON")} at @s run function ${pollOne.asId()}")
			raw("execute if entity ${block.markerSel("tag=$ON", "limit=1")} run function ${pollStart.asId()}")
		}
		pack.define(pollOne) {
			raw("function ${prep.asId()}")
			raw("execute if score #au.j $T matches 1 if score #au.has $T matches 1 if score #au.play $T matches 0 run function ${ended.asId()}")
			raw("function ${show.asId()}")
		}
		pack.define(ended) {
			raw("execute unless data entity @s data.properties{continuous:1b} run return run tag @s remove $ON")
			raw("function ${next.asId()}")
		}
	}
}
