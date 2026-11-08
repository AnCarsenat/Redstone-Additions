package ra.hub.uninstall

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.resources.storage
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.schedules
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.arguments.types.EntityTypeArgument
import ra.api.Settings
import ra.core.RA_CHAT_PREFIX
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.hub.tick
import ra.modules.chunkloader.ChunkLoader

/**
 * Objectives only old worlds have (`ra.edit_step`, `ra.filter_cd`...), in the order beet removed
 * them. Hand-written in beet and kept: nothing in the pack adds these any more, so the generated
 * list below cannot know them. Removing an objective that does not exist is harmless.
 */
private val LEGACY_OBJECTIVES = listOf(
	"ra.edit_type", "ra.edit_step", "ra.dh.action", "ra.dh.pending", "ra.input_handler.action",
	"ra.cooldown", "ra.filter_cd", "ra.temp", "ra.facing", "ra.yaw", "ra.pitch",
	"ra.power", "ra.power.up", "ra.power.down", "ra.power.north", "ra.power.south", "ra.power.east",
	"ra.power.west", "ra.power.front", "ra.power.back", "ra.power.left", "ra.power.right",
	"ra.power.local_up", "ra.power.local_down",
	"ra.inv.slot", "ra.inv.count", "ra.channel", "ra.pulse_timer", "ra.remote.pending", "ra.remote.slot",
	"ra.multiblock", "ra.mb_timer", "ra.mb_enabled", "ra.craft_id", "ra.heat",
	"ra.input.state", "ra.input.backend", "ra.input.mode", "ra.input.min", "ra.input.max",
	"ra.input.result", "ra.input.ttl", "ra.input.req", "ra.input.next", "ra.input.last",
	"ra.input.value", "ra.input.trigger",
	"ra.wires.tmp", "ra.wires.tmp2", "ra.wrench", "ra.wr.x", "ra.wr.y", "ra.wr.z",
)

/**
 * Every entity type the pack summons and keeps (everything it summons carries [Tag.RA]). Items
 * are left alone on purpose: a dropped custom block lying on the ground is a player's item.
 */
private val PACK_ENTITY_TYPES = listOf(
	EntityTypes.MARKER, EntityTypes.TEXT_DISPLAY, EntityTypes.ITEM_DISPLAY, EntityTypes.BLOCK_DISPLAY,
)

private val PLAYER_TAGS = listOf(
	"ra.wrench_active", "ra.cdh_active", "ra.dh_active", "ra.goggles_active", "ra.wrench_clicked",
	"ra.cdh_clicked", "ra.dh_clicked", "ra.remote_active", "ra.remote_clicked",
	"ra.input_handler_active", "ra.input_handler_clicked", "ra.input.active", Tag.DEBUG,
)

/**
 * Full cleanup: removes ALL Redstone Additions data from the world. From the [ UNINSTALL ] button.
 *
 * Called after every part has registered: the objective and storage lists are read back from the
 * finished pack by [PackFootprint], so a module adding an objective is covered without touching
 * this file.
 */
internal fun DataPack.registerUninstallConfirm() {
	val settingsRemoves = functions.firstOrNull { it.asId() == Settings.uninstall.asId() }?.lines.orEmpty()
		.mapNotNull { Regex("""^scoreboard objectives remove (\S+)$""").find(it)?.groupValues?.get(1) }
		.toSet()
	val generatedObjectives = PackFootprint.objectives(this)
		.filter { it !in LEGACY_OBJECTIVES && it !in settingsRemoves }
	val storageKeys = PackFootprint.storageKeys(this)

	define(confirm) {
		fun killAll(type: EntityTypeArgument, tag: String) = kill(allEntities { this.type = type; this.tag = tag })
		fun untagMarkers(tag: String) = tag(allEntities { type = EntityTypes.MARKER; this.tag = tag }) { remove(tag) }

		// FORCELOADS FIRST. A powered Chunk Loader holds its chunk until its on_break or
		// on_unpowered says otherwise, and killing the marker runs neither: the chunk would stay
		// loaded for ever, with nothing left in the world that knows why. `ra.was_powered` is
		// exactly the set that forceloaded (see ChunkLoader.kt), so a chunk an operator forceloaded
		// by hand next to an unpowered loader is left alone.
		raw("execute as @e[type=minecraft:marker,tag=${ChunkLoader.block.tag},tag=ra.was_powered] at @s run forceload remove ~ ~")

		// Markers, goggles billboards (text and item lines), the Clock and UNI Gate's floating
		// items, skins, pipe and wire displays.
		PACK_ENTITY_TYPES.forEach { killAll(it, Tag.RA) }

		LEGACY_OBJECTIVES.forEach { scoreboard.objectives.remove(it) }

		// Generated, so it lists exactly what ra_settings created.
		function(Settings.uninstall)

		generatedObjectives.forEach { scoreboard.objectives.remove(it) }

		// KEY BY KEY. `data remove storage <id> {}` changes nothing: `{}` is a root match, and
		// the game neither removes nor replaces the root of a storage. Removing each top-level key
		// is what actually empties it.
		for ((id, keys) in storageKeys) {
			val st = storage(id.substringAfter(':'), id.substringBefore(':'))
			keys.forEach { key -> data(st) { remove(key) } }
		}

		schedules { clear(tick) }

		PLAYER_TAGS.forEach { t -> tag(allPlayers()) { remove(t) } }
		untagMarkers("ra.dh_target")
		untagMarkers("ra.dh_selected")
		tag(allPlayers()) { remove("ra.wrench_user") }
		untagMarkers("ra.wires.node")
		untagMarkers("ra.wrench.sel")
		untagMarkers("ra.wires.fluid_node")
		untagMarkers("ra.wires.liquid_node")
		untagMarkers("ra.wires.gas_node")
		untagMarkers("ra.wires.electric_node")
		untagMarkers("ra.wires.legacy_gas_pipe")
		tag(allPlayers()) { remove("ra.settings_admin") }
		tag(allPlayers()) { remove("ra.admin") }

		raw("""tellraw @a [{text:"\n"},$RA_CHAT_PREFIX,{text:"Redstone Additions has been uninstalled.",color:"red"}]""")
		raw("""tellraw @a [{text:"  "},{text:"You may now disable the datapack with: ",color:"gray"},{text:"/datapack disable \"file/redstone_additions\"",color:"yellow",click_event:{action:"suggest_command",command:"/datapack disable \"file/redstone_additions\""},hover_event:{action:"show_text",value:"Click to run"}}]""")
	}
}
