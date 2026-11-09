package ra.tools.datahandler.props

import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.tool

/**
 * A text that must be unique in the world ([ra.core.PropSpec.unique]): a Teleport Anchor's id.
 *
 * The declaration names a compound, `unique:{storage:"ra:ender",path:"anchors"}`, whose keys are
 * the values already in use. A typed value that is a key there is refused, unless it is the block's
 * own current value (typing an anchor's id again changes nothing). The compound is the block's to
 * keep up to date; the Data Handler only reads it.
 *
 * The typed value becomes part of a path (`anchors."<value>"`), which is safe only because the
 * declaration requires format ID or TAG and [check] runs after the format check: no quote, no
 * backslash, no space.
 */
object DataHandlerUnique {
	/** As the player, after probe and the format check, storage ra:dh q {name, text}: 1 fine, 0 refused (and says why). */
	val check = tool("data_handler/props/check_unique")
	private val taken = tool("data_handler/props/unique_taken")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val TARGET = DataHandlerProps.TARGET

	fun Ra.registerDataHandlerUnique() {
		pack.define(check) {
			raw("execute unless data storage ra:dh spec.unique run return 1")
			raw("data remove storage ra:dh uq")
			// The block's own value is no clash. `set from` changes nothing on an equal value.
			raw("\$data modify storage ra:dh uq.cur set from entity $TARGET data.properties.\$(name)")
			raw("execute store success score #dh.uc $T run data modify storage ra:dh uq.cur set from storage ra:dh q.text")
			raw("execute if score #dh.uc $T matches 0 run data remove storage ra:dh uq")
			raw("execute if score #dh.uc $T matches 0 run return 1")
			raw("data modify storage ra:dh uq.storage set from storage ra:dh spec.unique.storage")
			raw("data modify storage ra:dh uq.path set from storage ra:dh spec.unique.path")
			raw("data modify storage ra:dh uq.key set from storage ra:dh q.text")
			raw("execute store result score #dh.uq $T run function ${taken.asId()} with storage ra:dh uq")
			raw("data remove storage ra:dh uq")
			raw("execute if score #dh.uq $T matches 0 run return 1")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Another block already has the ",color:"red"},{text:"$(name)",color:"yellow"},{text:" ",color:"red"},{nbt:"q.text",storage:"ra:dh",color:"aqua"},{text:": each one must be different. Nothing was changed.",color:"red"}]""")
			raw("playsound minecraft:block.note_block.bass block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.7")
			raw("return 0")
		}
		// {storage, path, key}: 1 when the key is in use.
		pack.define(taken) {
			raw("\$execute if data storage \$(storage) \$(path).\"\$(key)\" run return 1")
			raw("return 0")
		}
	}
}
