package ra.hub.uninstall

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.allPlayers
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
import ra.core.Storage
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.hub.tick

/**
 * Every objective the pack created before settings existed, in the order beet removed them.
 * Hand-written in beet and kept verbatim: removing an objective that does not exist is harmless,
 * and some of these (ra.edit_step, ra.filter_cd...) only exist in old worlds.
 */
private val OBJECTIVES = listOf(
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

/** Added after the list above was written. Two repeat entries of it, harmlessly. */
private val LATE_OBJECTIVES = listOf(
	"ra.clip.id", "ra.dh.pending", "ra.edit_type", "ra.dh.mid", "ra.dh.sel", "ra.dh.req", "ra.remote.req",
)

private val STORAGES = listOf(
	Storage.MULTIBLOCK, Storage.TEMP, Storage.BLOCK, Storage.CDH, Storage.DH, Storage.INPUT,
	Storage.WIRES, Storage.WRENCH, Storage.TRANSPORT, Storage.INVENTORY,
)

private val PLAYER_TAGS = listOf(
	"ra.wrench_active", "ra.cdh_active", "ra.dh_active", "ra.goggles_active", "ra.wrench_clicked",
	"ra.cdh_clicked", "ra.dh_clicked", "ra.remote_active", "ra.remote_clicked",
	"ra.input_handler_active", "ra.input_handler_clicked", "ra.input.active", Tag.DEBUG,
)

/** Full cleanup: removes ALL Redstone Additions data from the world. From the [ UNINSTALL ] button. */
internal fun DataPack.registerUninstallConfirm() = define(confirm) {
	fun killAll(type: EntityTypeArgument, tag: String) = kill(allEntities { this.type = type; this.tag = tag })
	fun untagMarkers(tag: String) = tag(allEntities { type = EntityTypes.MARKER; this.tag = tag }) { remove(tag) }

	killAll(EntityTypes.MARKER, Tag.CUSTOM_BLOCK)
	killAll(EntityTypes.MARKER, Tag.MULTIBLOCK)
	killAll(EntityTypes.TEXT_DISPLAY, Tag.BILLBOARD)
	killAll(EntityTypes.TEXT_DISPLAY, Tag.DISPLAY)

	OBJECTIVES.forEach { scoreboard.objectives.remove(it) }

	// Generated, so it lists exactly what ra_settings created. The hand-written list above is why
	// this is not another hand-written list.
	function(Settings.uninstall)

	LATE_OBJECTIVES.forEach { scoreboard.objectives.remove(it) }

	STORAGES.forEach { data(it) { remove("{}") } }

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
	killAll(EntityTypes.BLOCK_DISPLAY, "ra.skin")
	tag(allPlayers()) { remove("ra.settings_admin") }
	tag(allPlayers()) { remove("ra.admin") }
	killAll(EntityTypes.BLOCK_DISPLAY, "ra.wires.pipe_display")
	killAll(EntityTypes.BLOCK_DISPLAY, "ra.wires.wire_display")

	raw("""tellraw @a [{text:"\n"},$RA_CHAT_PREFIX,{text:"Redstone Additions has been uninstalled.",color:"red"}]""")
	raw("""tellraw @a [{text:"  "},{text:"You may now disable the datapack with: ",color:"gray"},{text:"/datapack disable \"file/redstone_additions\"",color:"yellow",click_event:{action:"suggest_command",command:"/datapack disable \"file/redstone_additions\""},hover_event:{action:"show_text",value:"Click to run"}}]""")
}
