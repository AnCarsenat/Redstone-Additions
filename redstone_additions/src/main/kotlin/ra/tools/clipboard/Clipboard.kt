package ra.tools.clipboard

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.advancement
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import net.benwoodworth.knbt.NbtByte
import ra.api.Goggles
import ra.api.RaLib
import ra.core.Admin.raiseMaxForAdmin
import ra.core.TextFormat
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.tools.raycast
import ra.tools.ToolItems
import ra.tools.tool

/**
 * The Clipboard: copy one block's settings and paste them onto others of the same kind.
 *
 * EVERYTHING IS SHIFT+RMB, ON PURPOSE
 * A plain right-click cannot be used here. Most of the blocks worth configuring are backed by a
 * container -- barrels, dispensers, droppers, furnaces -- and a plain click opens their GUI. Binding
 * anything to it would mean a tool that sometimes configures a block and sometimes opens a chest,
 * depending on what the block happens to be made of. Sneaking suppresses block interaction, so shift
 * is the only click that reliably belongs to the tool.
 *
 * THE MODEL
 * The first block you shift-click becomes the origin. Every block you shift-click after that is
 * given the origin's settings, as long as it is the same kind. So configuring twenty drains is: set
 * one up, shift-click it, then shift-click the other nineteen. Shift-click at nothing to clear.
 *
 * WHERE THE PAYLOAD LIVES
 * Not on the item: /data refuses to modify a player, so there is no way to write it back into the
 * stack you are holding. Not in one global slot either, or two players on a server would paste each
 * other's settings. A small integer per player (`ra.clip.id`), assigned on first use, keys a slot in
 * `storage ra:clipboard boards` and never needs cleaning up.
 */
object Clipboard {
	val give = tool("clipboard/give")
	val onUse = tool("clipboard/on_use")
	val ensureId = tool("clipboard/ensure_id")
	val load = tool("clipboard/load")
	val store = tool("clipboard/store")
	val clear = tool("clipboard/clear")
	val clearSlot = tool("clipboard/clear_slot")
	val copy = tool("clipboard/copy")
	val paste = tool("clipboard/paste")
	val checkKind = tool("clipboard/check_kind")
	val apply = tool("clipboard/apply")
	val refuse = tool("clipboard/refuse")
	private val strip = tool("clipboard/strip")
	private val stripNext = tool("clipboard/strip_next")
	private val stripOne = tool("clipboard/strip_one")
	private val stripText = tool("clipboard/strip_text")
	private val stripNumber = tool("clipboard/strip_number")
	private val stripVec3 = tool("clipboard/strip_vec3")

	private const val HOLDER = "@a[tag=ra.clip_active,limit=1]"
	private const val PREFIX = """{text:"[Clipboard] ",color:"gold"}"""

	fun Ra.registerClipboard() {
		pack.define(give) {
			raw("give @s ${ToolItems.clipboard.spec.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Clipboard",color:"gold"}]""")
		}

		// As the player, from the ra:tools/clipboard_use advancement.
		pack.define(onUse) {
			advancement { revoke(self(), AdvancementArgument("tools/clipboard_use", "ra")) }
			tag(self()) { add("ra.clip_clicked") }
			raw("execute if entity @s[tag=ra.clip_active] run return fail")
			tag(self()) { add("ra.clip_active") }
			// Not sneaking: the player is interacting with a block, not using the tool.
			raw("execute unless predicate ra:is_sneaking run return 0")
			function(ensureId)
			// Is the clipboard loaded? That, not the click, decides copy versus apply.
			raw("execute store result storage ra:temp clip.slot int 1 run scoreboard players get @s ra.clip.id")
			data(Storage.TEMP) { remove("clip.kind") }
			callWith(load, "storage ra:temp clip")
			data(Storage.TEMP) { remove("clip_found") }
			// No "nothing there" complaint from the cast: a miss is meaningful here (see below).
			raycast("clip_found", paste, guard = "if data storage ra:temp clip.kind ")
			raycast("clip_found", copy, guard = "unless data storage ra:temp clip.kind ")
			// Nothing in front of you is the deliberate way to empty it. Clearing on a miss rather than
			// on a hit means a slightly-off click can never wipe a working origin.
			executeRun("unless data storage ra:temp clip_found", clear)
			data(Storage.TEMP) { remove("clip_found") }
		}

		// Give this player a slot number if they do not have one yet. As the player.
		pack.define(ensureId) {
			raw("execute unless score @s ra.clip.id matches 1.. run scoreboard players add #clip.next ${Obj.TEMP} 1")
			raw("execute unless score @s ra.clip.id matches 1.. run scoreboard players operation @s ra.clip.id = #clip.next ${Obj.TEMP}")
		}

		// {slot}: pull this player's slot back into scratch.
		pack.define(load) {
			for (field in listOf("kind", "props"))
				raw("\$execute if data storage ra:clipboard boards.p\$(slot) run data modify storage ra:temp clip.$field set from storage ra:clipboard boards.p\$(slot).$field")
		}
		// {slot,kind,props}: write the payload into this player's slot.
		pack.define(store) {
			raw("\$data modify storage ra:clipboard boards.p\$(slot) set value {}")
			raw("\$data modify storage ra:clipboard boards.p\$(slot).kind set from storage ra:temp clip.kind")
			raw("\$data modify storage ra:clipboard boards.p\$(slot).props set from storage ra:temp clip.props")
		}

		// Empty this player's clipboard. As the player.
		pack.define(clear) {
			raw("execute store result storage ra:temp clip.slot int 1 run scoreboard players get @s ra.clip.id")
			callWith(clearSlot, "storage ra:temp clip")
			data(Storage.TEMP) { remove("clip") }
			raw("""tellraw @s [$PREFIX,{text:"Cleared. The next block you click becomes the new origin.",color:"gray"}]""")
			raw("playsound minecraft:item.book.put block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.9")
		}
		pack.define(clearSlot) { raw("\$data remove storage ra:clipboard boards.p\$(slot)") }


		registerCopyPaste()
	}

	private fun Ra.registerCopyPaste() {
		// Make this block the origin: its settings become what every later click hands out.
		// As the target block's marker.
		pack.define(copy) {
			data(Storage.TEMP) { modify("clip_found", NbtByte(1)) }
			// Only data.properties travels. data.data is a block's private working state -- a pump's
			// cooldown, a gate's latch, a network id -- and copying that onto another block would hand
			// it a half-finished operation belonging to somewhere else. Settings are what a player
			// means by "these settings".
			raw("""execute unless data entity @s data.properties run return run tellraw $HOLDER [$PREFIX,{text:"That block has no settings to copy.",color:"gray"}]""")
			// The name doubles as the paste guard, so a Pump's settings cannot be pasted onto a Gate.
			// Taken from the goggles name dispatch, which is the single source.
			blockNameOnly()
			raw("data modify storage ra:temp clip.props set from entity @s data.properties")
			raw("data modify storage ra:temp clip.kind set from storage ra:temp block_name")
			raw("execute store result storage ra:temp clip.slot int 1 run scoreboard players get $HOLDER ra.clip.id")
			callWith(store, "storage ra:temp clip")
			raw("""tellraw $HOLDER [$PREFIX,{text:"Origin set: ",color:"gray"},{nbt:"block_name",storage:"ra:temp",color:"yellow"},{text:". Click others of the same kind to match it.",color:"gray"}]""")
			raw("playsound minecraft:item.book.page_turn block @a[tag=ra.clip_active,limit=1,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.8 1.6")
			data(Storage.TEMP) { remove("clip") }
		}

		// Apply the holder's clipboard onto this block. As the target block's marker.
		pack.define(paste) {
			data(Storage.TEMP) { modify("clip_found", NbtByte(1)) }
			raw("execute store result storage ra:temp clip.slot int 1 run scoreboard players get $HOLDER ra.clip.id")
			data(Storage.TEMP) { remove("clip.kind") }
			callWith(load, "storage ra:temp clip")
			raw("""execute unless data storage ra:temp clip.kind run return run tellraw $HOLDER [$PREFIX,{text:"No origin set — click a block to choose one.",color:"gray"}]""")
			// Same kind only. Property names overlap between unrelated blocks -- half the pack once
			// had `enabled` -- so without this a Gas Valve's rate would happily land on a Randomizer's
			// chance and quietly mean something else.
			blockNameOnly()
			callWith(checkKind, "storage ra:temp clip")
			executeReturnRun("unless data storage ra:temp clip_ok", refuse)
			function(strip)
			callWith(apply, "storage ra:temp clip")
			raw("""tellraw $HOLDER [$PREFIX,{text:"Matched this ",color:"gray"},{nbt:"block_name",storage:"ra:temp",color:"yellow"},{text:" to the origin.",color:"gray"}]""")
			raw("playsound minecraft:block.amethyst_block.chime block @a[tag=ra.clip_active,limit=1,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 1.4")
			raw("particle minecraft:happy_villager ~ ~0.8 ~ 0.3 0.3 0.3 0.02 6 normal @a[scores={${Obj.USER_PARTICLES}=1..}]")
			data(Storage.TEMP) { remove("clip") }
			data(Storage.TEMP) { remove("clip_ok") }
		}

		// {kind}: does the copied block's name match the one being pasted onto? Sets storage ra:temp
		// clip_ok when it does. A macro because this compares two runtime strings, which no block or
		// entity predicate can do -- one arrives as the macro argument, the other is read out of
		// storage where the name dispatch just left it.
		pack.define(checkKind) {
			raw("\$execute if data storage ra:temp {block_name:\"\$(kind)\"} run data modify storage ra:temp clip_ok set value 1b")
		}

		// {props}: merge the copied settings onto this block. As the target marker.
		// A merge rather than a wholesale set, so a property the source block did not carry is left
		// alone on the target instead of being deleted. Two blocks of the same kind can still differ
		// in what they have if one was placed by an older version of the pack.
		pack.define(apply) {
			raw("data merge entity @s {data:{properties:{}}}")
			raw("data modify entity @s data.properties merge from storage ra:temp clip.props")
		}

		// Take out of the copied settings what the holder may not set on this block, by its type's
		// schema: read-only values (the target keeps its own working numbers), and admin-only or
		// hidden ones unless the holder has server-settings access. As the target marker.
		pack.define(strip) {
			function(ra.tools.datahandler.DataHandler.loadSchema)
			raw("execute store success score #clip.adm ${Obj.TEMP} if entity @a[tag=ra.clip_active,tag=ra.admin,limit=1]")
			raw("data modify storage ra:temp clip_iter set from storage ra:dh block_order")
			function(stripNext)
			raw("data remove storage ra:temp clip_iter")
		}
		pack.define(stripNext) {
			raw("execute unless data storage ra:temp clip_iter[0] run return 0")
			raw("data modify storage ra:temp clip_q set value {}")
			raw("data modify storage ra:temp clip_q.name set from storage ra:temp clip_iter[0]")
			callWith(stripOne, "storage ra:temp clip_q")
			raw("data remove storage ra:temp clip_iter[0]")
			function(stripNext)
		}
		// {name}
		pack.define(stripOne) {
			raw("\$execute store result score #clip.ro ${Obj.TEMP} run data get storage ra:dh schema.\$(name).readonly")
			raw("\$execute store result score #clip.ao ${Obj.TEMP} run data get storage ra:dh schema.\$(name).admin_only")
			// Admin-only unless its server setting lets every player edit it (the Data Handler's rule).
			raw("scoreboard players set #clip.wv ${Obj.TEMP} 0")
			raw("\$execute if score #clip.ao ${Obj.TEMP} matches 1 if data storage ra:dh schema.\$(name).admin_setting store result score #clip.wv ${Obj.TEMP} run function ${ra.tools.datahandler.props.DataHandlerEntries.waived.asId()} with storage ra:dh schema.\$(name)")
			raw("execute if score #clip.wv ${Obj.TEMP} matches 1 run scoreboard players set #clip.ao ${Obj.TEMP} 0")
			raw("scoreboard players set #clip.vis ${Obj.TEMP} 1")
			raw("\$execute if data storage ra:dh schema.\$(name).visible store result score #clip.vis ${Obj.TEMP} run data get storage ra:dh schema.\$(name).visible")
			raw("\$execute if score #clip.ro ${Obj.TEMP} matches 1 run return run data remove storage ra:temp clip.props.\$(name)")
			// A number in its range, as the Data Handler would have taken it: the players' maximum,
			// or an admin's admin_max (ra.core.Admin). An admin's 200-block torch is not copied by a
			// player onto theirs.
			raw("\$execute if data storage ra:dh schema.\$(name){code:1} run function ${stripNumber.asId()} {name:\"\$(name)\"}")
			raw("\$execute if data storage ra:dh schema.\$(name){code:6} run function ${stripVec3.asId()} {name:\"\$(name)\"}")
			raw("execute if score #clip.adm ${Obj.TEMP} matches 1 run return 0")
			raw("\$execute if score #clip.ao ${Obj.TEMP} matches 1 run return run data remove storage ra:temp clip.props.\$(name)")
			raw("\$execute if score #clip.vis ${Obj.TEMP} matches 0 run return run data remove storage ra:temp clip.props.\$(name)")
			// Text in its format, as the Data Handler would have taken it (ra_lib:safe/check): what a
			// player could not type, they cannot paste either -- a Message Block's rich text (a
			// compound) among it.
			raw("\$execute if data storage ra:dh schema.\$(name){code:0} run function ${stripText.asId()} {name:\"\$(name)\"}")
			raw("\$execute if data storage ra:dh schema.\$(name){code:4} run function ${stripText.asId()} {name:\"\$(name)\"}")
		}
		// {name}: drop clip.props.<name> when it is outside the declared bounds for the holder.
		pack.define(stripNumber) {
			raw("data remove storage ra:temp clip_spec")
			raw("\$data modify storage ra:temp clip_spec set from storage ra:dh schema.\$(name)")
			raw("scoreboard players set #clip.min ${Obj.TEMP} -2147483648")
			raw("scoreboard players set #clip.max ${Obj.TEMP} 2147483647")
			raw("execute if data storage ra:temp clip_spec.min store result score #clip.min ${Obj.TEMP} run data get storage ra:temp clip_spec.min")
			raw("execute if data storage ra:temp clip_spec.max store result score #clip.max ${Obj.TEMP} run data get storage ra:temp clip_spec.max")
			raiseMaxForAdmin("storage ra:temp clip_spec", "#clip.max", Obj.TEMP, "if score #clip.adm ${Obj.TEMP} matches 1")
			raw("data remove storage ra:temp clip_spec")
			raw("\$execute store result score #clip.v ${Obj.TEMP} run data get storage ra:temp clip.props.\$(name)")
			raw("\$execute unless score #clip.v ${Obj.TEMP} >= #clip.min ${Obj.TEMP} run data remove storage ra:temp clip.props.\$(name)")
			raw("\$execute unless score #clip.v ${Obj.TEMP} <= #clip.max ${Obj.TEMP} run data remove storage ra:temp clip.props.\$(name)")
		}
		// {name}: a vec3 (ra.core.Vec3Props) kept only inside the holder's range and on its grid,
		// and rewritten as three integers. A world position (frame 2) is not copied: its range is
		// measured from the block, and the target block is somewhere else.
		pack.define(stripVec3) {
			val t = Obj.TEMP
			raw("data remove storage ra:temp clip_spec")
			raw("\$data modify storage ra:temp clip_spec set from storage ra:dh schema.\$(name).v3")
			raw("\$execute if data storage ra:temp clip_spec{frame:2} run return run data remove storage ra:temp clip.props.\$(name)")
			raw("\$execute unless data storage ra:temp clip.props.\$(name) run return 0")
			raw("execute store result score #clip.g $t run data get storage ra:temp clip_spec.grid")
			raw("execute unless score #clip.g $t matches 1.. run scoreboard players set #clip.g $t 1")
			for ((i, a) in listOf("x", "y", "z").withIndex()) {
				raw("\$execute store result score #clip.v $t run data get storage ra:temp clip.props.\$(name).$a")
				raw("execute store result score #clip.min $t run data get storage ra:temp clip_spec.min[$i]")
				raw("execute store result score #clip.max $t run data get storage ra:temp clip_spec.max[$i]")
				raw("execute if score #clip.adm $t matches 1 if data storage ra:temp clip_spec.amin store result score #clip.min $t run data get storage ra:temp clip_spec.amin[$i]")
				raw("execute if score #clip.adm $t matches 1 if data storage ra:temp clip_spec.amax store result score #clip.max $t run data get storage ra:temp clip_spec.amax[$i]")
				raw("\$execute unless score #clip.v $t >= #clip.min $t run return run data remove storage ra:temp clip.props.\$(name)")
				raw("\$execute unless score #clip.v $t <= #clip.max $t run return run data remove storage ra:temp clip.props.\$(name)")
				raw("scoreboard players operation #clip.r $t = #clip.v $t")
				raw("scoreboard players operation #clip.r $t %= #clip.g $t")
				raw("\$execute unless score #clip.r $t matches 0 run return run data remove storage ra:temp clip.props.\$(name)")
				raw("scoreboard players operation #clip.$a $t = #clip.v $t")
			}
			raw("\$data modify storage ra:temp clip.props.\$(name) set value {x:0,y:0,z:0}")
			for (a in listOf("x", "y", "z")) raw("\$execute store result storage ra:temp clip.props.\$(name).$a int 1 run scoreboard players get #clip.$a $t")
			raw("data remove storage ra:temp clip_spec")
		}
		// {name}: drop clip.props.<name> unless it is a string in its schema format.
		pack.define(stripText) {
			raw("data remove storage ra:safe s")
			raw("\$data modify storage ra:safe s set from storage ra:temp clip.props.\$(name)")
			raw("execute unless data storage ra:safe s run return 0")
			raw("data modify storage ra:safe f set value ${TextFormat.ID.code}")
			raw("\$execute if data storage ra:dh schema.\$(name).fmt run data modify storage ra:safe f set from storage ra:dh schema.\$(name).fmt")
			raw("execute store result score #clip.fok ${Obj.TEMP} run function ${RaLib.Safe.check.asId()}")
			raw("\$execute if score #clip.fok ${Obj.TEMP} matches 0 run data remove storage ra:temp clip.props.\$(name)")
		}

		// Say why the paste did not happen, naming both kinds.
		pack.define(refuse) {
			raw("""tellraw $HOLDER [$PREFIX,{text:"Origin is a ",color:"gray"},{nbt:"clip.kind",storage:"ra:temp",color:"yellow"},{text:" — that is a ",color:"gray"},{nbt:"block_name",storage:"ra:temp",color:"yellow"},{text:".",color:"gray"}]""")
			raw("playsound minecraft:block.note_block.bass block @a[tag=ra.clip_active,limit=1,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.7")
			data(Storage.TEMP) { remove("clip") }
			data(Storage.TEMP) { remove("clip_ok") }
		}
	}

	/** Ask this marker's goggles function for its name only: into storage ra:temp block_name. */
	private fun io.github.ayfri.kore.functions.Function.blockNameOnly() {
		data(Storage.TEMP) { modify("name_only", NbtByte(1)) }
		function(Goggles.drawBlock)
		data(Storage.TEMP) { remove("name_only") }
	}
}
