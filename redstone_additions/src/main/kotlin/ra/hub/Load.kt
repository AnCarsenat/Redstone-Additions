package ra.hub

import io.github.ayfri.kore.arguments.scores.ScoreboardCriteria
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.RedstoneAdditions
import ra.api.Goggles
import ra.api.ModulesApi
import ra.api.MultiblockLib
import ra.api.RaLib
import ra.api.Settings
import ra.api.Tools
import ra.core.Ns
import ra.core.Obj
import ra.core.RA_CHAT_PREFIX
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.hook
import ra.core.loadMessage
import ra.core.loadNotice
import ra.core.ref
import ra.hub.items.giveAllItems
import io.github.ayfri.kore.arguments.numbers.ticks

/** `ra:load`, run by `#minecraft:load`. Initializes scoreboards, libraries and every module. */
internal val load = ref(Ns.RA, "load")

/** `ra:tick`. Not in `#minecraft:tick`: it reschedules itself, see [registerTick]. */
internal val tick = ref(Ns.RA, "tick")

// Tool menu objectives. Shared with ra:tools, which reads and resets them.
internal const val EDIT_TYPE = "ra.edit_type"
internal const val DH_ACTION = "ra.dh.action"
internal const val DH_PENDING = "ra.dh.pending"
internal const val HOLD_WRENCH = "ra.hold.wrench"
internal const val HOLD_DH = "ra.hold.dh"
internal const val CLIP_ID = "ra.clip.id"
internal const val WRENCH = "ra.wrench"
internal const val WR_X = "ra.wr.x"
internal const val WR_Y = "ra.wr.y"
internal const val WR_Z = "ra.wr.z"

internal fun Ra.registerLoad() {
	pack.hook(Ns.MINECRAFT, "load", load)

	pack.define(load) {
		scoreboards()

		// Shared temp storage used by macros and status rendering.
		data(Storage.TEMP) {
			merge(nbt {
				this["has_facing"] = 0.toByte()
				this["facing_name"] = "north"
				this["block_id"] = ""
				this["io"] = nbt {}
				this["status_literal"] = nbt {}
				this["billboard"] = nbt {}
				this["goggles_props"] = nbt {}
				this["upgrade"] = nbt {}
			})
		}

		// Configurable text-display offsets, once.
		function(Goggles.Billboard.initOffsets)

		function(RaLib.init)

		for (module in RedstoneAdditions.modules) function(module.load)

		function(MultiblockLib.init)

		// The wrench menu: which row was clicked, and which block it belongs to.
		scoreboard.objectives.add(WRENCH, ScoreboardCriteria.TRIGGER)
		scoreboard.objectives.add(WR_X)
		scoreboard.objectives.add(WR_Y)
		scoreboard.objectives.add(WR_Z)

		// Settings. After the modules have loaded, because each module registers its own page
		// through #ra_settings:register and cannot do that before it exists.
		function(Settings.init)

		function(Tools.Wrench.initRegistry)
		function(Tools.Readonly.initRegistry)
		function(Tools.DataHandler.initRegistry)

		// Bring a world saved by an older version up to date: the world steps it has not had yet.
		// Its blocks are brought up to date one by one from ra:tick. See ra.migrations.MigrationsPart.
		function(ModulesApi.Migrations.run)

		// Next tick rather than now, to avoid a load-time command burst. `replace` is what a bare
		// `schedule function` does; Kore always spells the mode out.
		schedule(tick).replace(1.ticks)

		disabledBlocksNotice()
		welcome()
	}
}

private fun Function.scoreboards() {
	// Data handler scoreboards.
	scoreboard.objectives.add(EDIT_TYPE, ScoreboardCriteria.TRIGGER)
	scoreboard.objectives.add(DH_ACTION, ScoreboardCriteria.TRIGGER)
	scoreboard.objectives.add(DH_PENDING)
	// Per-player selection and pending edit: see ra.tools.datahandler.DataHandlerSelection.
	scoreboard.objectives.add(ra.tools.datahandler.DataHandlerSelection.MID)
	scoreboard.objectives.add(ra.tools.datahandler.DataHandlerSelection.SEL)
	scoreboard.objectives.add(ra.tools.datahandler.DataHandlerSelection.REQ)

	// How long a tool's menu triggers stay usable after it was last in hand. An enabled trigger
	// appears in everyone's /trigger completion whether it can do anything or not, so they are
	// handed out while the tool is held and taken back when it is not -- with a grace window,
	// because putting the wrench away to read the menu it just printed should not disarm the
	// buttons.
	scoreboard.objectives.add(HOLD_WRENCH)
	scoreboard.objectives.add(HOLD_DH)
	// Clipboard slot number, assigned on first use. See ra:tools/clipboard/ensure_id.
	scoreboard.objectives.add(CLIP_ID)
	// The debug-only input handler objective (ra.input_handler.action) stays disabled, as in beet.
}

/**
 * A [loadNotice]: not subject to the load-message setting. A disabled block leaves no trace anywhere
 * a player looks -- it just refuses to place -- so a server that has some needs to be told on
 * every load, whether or not it wants the greeting.
 */
private fun Function.disabledBlocksNotice() {
	val dis = literal("#dis")
	execute {
		storeResult { score(dis, Obj.SET_TMP) }
		run { data(Storage.SETTINGS) { get("disabled") } }
	}
	loadNotice("""execute if score #dis ${Obj.SET_TMP} matches 1.. run tellraw @a [$RA_CHAT_PREFIX,{score:{name:"#dis",objective:"${Obj.SET_TMP}"},color:"red",bold:true},{text:" block type(s) are disabled and cannot be placed.",color:"red"}]""")
	loadNotice("""execute if score #dis ${Obj.SET_TMP} matches 1.. run tellraw @a [{text:"  "},{text:"[ Which ones? ]",color:"yellow",hover_event:{action:"show_text",value:"List them, with a button to re-enable each"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 3"}}]""")
}

/**
 * Gated on a setting, because a server that has run this pack for a year does not need to tell
 * everyone about it on every reload. Every line carries the gate itself ([loadMessage]) rather
 * than sitting behind one early `return`, the same gate the modules' "loaded!" lines use, so the
 * build can prove nothing said at load escapes it: see
 * [ra.core.PackValidator.ungatedLoadMessages].
 */
private fun Function.welcome() {
	val v = RedstoneAdditions.VERSION
	loadMessage("""[{text:"[RA_Lib] ",color:"gold"},{text:"v$v loaded",color:"green"}]""")
	loadMessage("""[$RA_CHAT_PREFIX,{text:"Redstone Additions v$v loaded!",color:"green"}]""")
	// The server button fires a TRIGGER, not the function. A /function link raises a
	// confirmation dialog and then fails for anyone without permission; the trigger opens the
	// panel for an operator who already has a session, and hands everyone else the command unrun
	// so the game can refuse them properly. See ra_settings:server_open.
	loadMessage("""[{text:"Server settings: ",color:"gray"},{text:"[ Open ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"World-wide settings — needs permission level 2"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 2"}}]""")
	// User settings are SUGGESTED rather than run, so the command lands in the chat box and the
	// player reads it before pressing enter. This one has to be remembered -- it is the only way
	// back into the menu once this message has scrolled away -- and a button that silently works
	// teaches nobody its name.
	loadMessage("""[{text:"Your settings: ",color:"gray"},{text:"/trigger ra.settings.open",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Click to put this in your chat box — then press enter"},click_event:{action:"suggest_command",command:"/trigger ra.settings.open"}}]""")
	loadMessage("""[{text:"Use ",color:"gray"},{text:"/function ${giveAllItems.asId()}",color:"yellow",hover_event:{action:"show_text",value:"Give all items"},click_event:{action:"suggest_command",command:"/function ${giveAllItems.asId()}"}},{text:" to get items",color:"gray"}]""")
}
