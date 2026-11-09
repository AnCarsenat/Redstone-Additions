package ra.core

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import ra.core.block.CustomBlock

/** Chat lines every module writes the same way. */

/** `[RA] ` in the pack's colours (dark-grey brackets, bold gold RA), as the start of a tellraw array. */
const val RA_CHAT_PREFIX = """{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"}"""

/** `tellraw <target> [<RA prefix>,{text:"<text>",color:"<color>"}]`. */
fun Function.raMessage(target: String, text: String, color: String) {
	raw("""tellraw $target [$RA_CHAT_PREFIX,{text:"$text",color:"$color"}]""")
}

/**
 * Everything `ra:load` says in chat goes through here, so the "Show the load message" server
 * setting (`global.welcome`) silences all of it -- the hub's greeting and every module's
 * "loaded!" line alike. [PackValidator.ungatedLoadMessages] fails the build on a tellraw reachable
 * from `#minecraft:load` that bypasses both [loadMessage] and [loadNotice].
 *
 * The gate reads the storage directly instead of going through `ra_settings:get`: module loads run
 * before `ra_settings:init` has created its objectives, and on a first install before anything
 * has, so a score-based gate would fail open there. The settings' own writers (`.../on`, `.../off`
 * and the defaults) only ever store `1b` or `0b`; a missing key is the default, on.
 */
object LoadMessages {
	/** The `execute` prefix of a gated line. */
	const val GATE = "execute unless data storage ra:settings global{welcome:0b} run "

	private val notices = mutableSetOf<String>()

	/** Lines registered through [loadNotice]: said on every load whatever the setting. */
	val alwaysShown: Set<String> get() = notices

	/** Records [line] as an intentional, ungated load notice. */
	internal fun notice(line: String) = line.also { notices += it }
}

/** `tellraw @a <json>`, unless an operator turned the load message off. */
fun Function.loadMessage(json: String) = raw("${LoadMessages.GATE}tellraw @a $json")

/**
 * A load-time line that is NOT subject to the load-message setting: a warning the server needs
 * whether or not it wants the greeting (disabled blocks, a failed step). Use sparingly -- the
 * build lists them as allowed only because they went through here.
 */
fun Function.loadNotice(line: String) = raw(LoadMessages.notice(line))

/** `[RA] <text>` to everyone, in the module-load colours: the line each module's `load` prints. */
fun Function.moduleLoaded(text: String, color: String) = loadMessage("""[$RA_CHAT_PREFIX,{text:"$text",color:"$color"}]""")

/**
 * `tellraw @a[tag=ra.debug] [RA] <name> registered`, the per-block debug line of every module
 * load. Only players tagged `ra.debug` see it, so `load` stays verbose for debugging only.
 */
fun Function.registeredLine(name: String) {
	raw("""tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"$name registered",color:"gray"}]""")
}

/** `ns:blocks/<dir>/register_block`: [registeredLine] for [block], called from the module's `load`. */
fun DataPack.blockRegisterMessage(block: CustomBlock): FunctionArgument = define(block.fn("register_block")) {
	registeredLine(block.name)
}
