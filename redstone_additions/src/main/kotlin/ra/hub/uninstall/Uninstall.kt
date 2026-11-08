package ra.hub.uninstall

import io.github.ayfri.kore.DataPack
import ra.api.Hub
import ra.core.Ns
import ra.core.RA_CHAT_PREFIX
import ra.core.Ra
import ra.core.define
import ra.core.raMessage
import ra.core.raw
import ra.core.ref

/**
 * `/function ra:uninstall`, two warnings, then [registerUninstallConfirm].
 *
 * Two prompts rather than one because this is unrecoverable and reachable from a button on the
 * settings index -- one misclick should not be able to end a world's worth of machines. The first
 * asks; the second says exactly what goes.
 */
internal val warn = ref(Ns.RA, "uninstall/warn")
internal val cancel = ref(Ns.RA, "uninstall/cancel")
internal val confirm = ref(Ns.RA, "uninstall/confirm")

private fun button(label: String, color: String, hover: String, target: String) =
	"""{text:"$label",color:"$color",bold:true,hover_event:{action:"show_text",value:"$hover"},click_event:{action:"run_command",command:"/function $target"}}"""

private val cancelButton get() = button("[ Cancel ]", "green", "Leave everything alone", cancel.asId())

internal fun Ra.registerUninstall() = with(pack) {
	// First of two warnings. Nothing is removed by this function.
	define(Hub.uninstall) {
		raw("""tellraw @s [{text:"\n"},$RA_CHAT_PREFIX,{text:"Uninstall Redstone Additions?",color:"red",bold:true}]""")
		raw("""tellraw @s [{text:"  This removes every custom block, marker and display in the world.",color:"gray"}]""")
		raw("""tellraw @s [{text:"  "},${button("[ Continue ]", "red", "You will be asked once more", warn.asId())},{text:"   "},$cancelButton]""")
	}

	// Second and final warning. Still removes nothing.
	//
	// This one is specific. "Are you sure" twice teaches nobody anything; a list of what is about
	// to be destroyed is what actually stops the wrong click.
	define(warn) {
		raw("""tellraw @s [{text:"\n"},$RA_CHAT_PREFIX,{text:"LAST WARNING — this cannot be undone.",color:"dark_red",bold:true}]""")
		raw("""tellraw @s [{text:"  Every placed machine becomes an ordinary block.",color:"gray"}]""")
		raw("""tellraw @s [{text:"  Every network, multiblock and goggle display is destroyed.",color:"gray"}]""")
		raw("""tellraw @s [{text:"  Every setting, including which blocks you disabled, is erased.",color:"gray"}]""")
		raw("""tellraw @s [{text:"  There is no backup. Close this and copy your world first if you want one.",color:"yellow"}]""")
		raw("""tellraw @s [{text:"  "},${button("[ UNINSTALL ]", "dark_red", "Destroy all Redstone Additions data now", confirm.asId())},{text:"   "},$cancelButton]""")
	}

	define(cancel) {
		raMessage("@a", "Uninstall cancelled.", "green")
	}

	// Once the pack is complete, after every part AND every afterAllParts step: the confirm step
	// reads the finished pack for what to remove, so a function defined from a deferred step
	// (registries, compiled hooks) must already exist. PackFootprint.uncovered re-checks it.
	whenPackComplete { registerUninstallConfirm() }}

/** What the finished pack creates and [confirm] leaves behind; see [PackFootprint.uncovered]. */
internal fun uninstallGaps(pack: DataPack): List<String> = PackFootprint.uncovered(pack, confirm.asId())
