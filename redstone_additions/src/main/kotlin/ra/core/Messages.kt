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

/** `[RA] <text>` to everyone, in the module-load colours: the line each module's `load` prints. */
fun Function.moduleLoaded(text: String, color: String) = raMessage("@a", text, color)

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
