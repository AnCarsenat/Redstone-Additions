package ra.lib.input

import io.github.ayfri.kore.functions.Function
import ra.core.Storage
import ra.core.raw

/** Everything the input sessions keep lives in `storage ra:input`. */
internal val IN = Storage.INPUT

/** Per-player objectives of the input sessions. Written into worlds, so spelled as in beet. */
internal object InputObj {
	/** 0 idle, 1 waiting, 2 ready. */
	const val STATE = "ra.input.state"
	/** 1 trigger, 2 writable book. */
	const val BACKEND = "ra.input.backend"
	/** 1 choice, 2 text, 3 number. */
	const val MODE = "ra.input.mode"
	const val MIN = "ra.input.min"
	const val MAX = "ra.input.max"
	const val RESULT = "ra.input.result"
	/** Ticks left before the session expires. */
	const val TTL = "ra.input.ttl"
	/** This player's request id; sessions live under `sessions.req_<id>`. */
	const val REQ = "ra.input.req"
	/** `#next ra.input.next` is the request id allocator. */
	const val NEXT = "ra.input.next"
	const val LAST = "ra.input.last"
	const val VALUE = "ra.input.value"
	/** The `trigger` objective a player submits a number with. */
	const val TRIGGER = "ra.input.trigger"

	/** On a player with a session open. */
	const val ACTIVE = "ra.input.active"

	/** Int-min sentinel on the trigger, so a submission of 0 is still a change. */
	const val SENTINEL = Int.MIN_VALUE
}

/** `execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req`. */
internal fun Function.storeReq(path: String = "tmp.req") =
	raw("execute store result storage ra:input $path int 1 run scoreboard players get @s ${InputObj.REQ}")

/** `[RA Input] <text>` to the executing player. */
internal fun Function.inputMessage(text: String, color: String) =
	raw("""tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"$text",color:"$color"}]""")
