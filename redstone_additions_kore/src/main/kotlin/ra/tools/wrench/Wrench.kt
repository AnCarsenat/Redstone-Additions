package ra.tools.wrench

import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.advancement
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnFail
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.REACH_4
import ra.tools.ToolItems
import ra.tools.executeReturnRun
import ra.tools.executeRun
import ra.tools.raycast
import ra.tools.tool

/** The Wrench: RMB toggles a block or assembles a multiblock, Shift+RMB cycles a block's settings. */
object Wrench {
	val give = tool("wrench/give")
	val onUse = tool("wrench/on_use")
	val rmbFunction = tool("wrench/rmb_function")
	val shiftRmbFunction = tool("wrench/shift_rmb_function")
	val foundBlock = tool("wrench/found_block")

	/** Multiblock bases answer to either tag: older ones were placed before the custom_block form. */
	private val BASE_TAGS = listOf("ra.multiblock_base", "ra.custom_block.multiblock_base")

	fun Ra.registerWrench() {
		pack.define(give) {
			raw("give @s ${ToolItems.wrench.argument}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Wrench",color:"gold"}]""")
		}
		pack.define(onUse) { onUse() }
		pack.define(rmbFunction) { rmb() }
		pack.define(shiftRmbFunction) { shiftRmb() }
		pack.define(foundBlock) { foundBlock(this@registerWrench) }
	}

	/** As the player, from the `ra:tools/wrench_use` advancement. */
	private fun Function.onUse() {
		advancement { revoke(self(), AdvancementArgument("tools/wrench_use", "ra")) }

		// Mark that player clicked this tick (for tag cleanup in ra:tick).
		tag(self()) { add("ra.wrench_clicked") }

		// If player is already holding (has tag), skip execution.
		raw("execute if entity @s[tag=ra.wrench_active] run return fail")

		// Mark player as actively using this tool.
		tag(self()) { add("ra.wrench_active") }

		executeReturnRun("if predicate ra:is_sneaking", shiftRmbFunction)
		function(rmbFunction)
	}

	/** RMB, not sneaking: toggle wireless blocks, or assemble/toggle multiblocks. As the player. */
	private fun Function.rmb() {
		raw("data remove storage ra:temp wrench_found")
		raycast(REACH_4, "wrench_found", foundBlock)

		// If no custom block found, try multiblock assembly/interaction at the looked-at block.
		for (step in REACH_4) executeRun(
			"unless data storage ra:temp wrench_found at @s anchored eyes positioned ^ ^ ^$step align xyz positioned ~0.5 ~ ~0.5",
			WrenchMultiblock.tryMultiblock,
		)

		raw("""execute unless data storage ra:temp wrench_found run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"No custom block or multiblock found nearby.",color:"gray"}]""")
		raw("data remove storage ra:temp wrench_found")
	}

	/** Shift+RMB: cycle the block in front of the player. As the player. */
	private fun Function.shiftRmb() {
		raw("data remove storage ra:temp wrench_found")
		// The menu and every cycler message this player specifically; the wrench runs `as` the
		// marker from here on and would otherwise have no idea who swung it.
		tag(self()) { add("ra.wrench_user") }
		// Half-block steps: the cycle menu should land on the block the crosshair is on, and the
		// first probe starts right at the eyes.
		raycast(listOf("0.5", "1.5", "2.5", "3.5", "4.5"), "wrench_found", WrenchMenu.cycleBlock)

		raw("""execute unless data storage ra:temp wrench_found run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"No cyclable block found nearby.",color:"gray"}]""")
		raw("data remove storage ra:temp wrench_found")

		tag(allPlayers()) { remove("ra.wrench_user") }
	}

	/**
	 * Plain RMB landed on a custom block. As its marker.
	 *
	 * The toggles come from the registry (`ra.wrenchToggles`): each block with a plain-RMB action
	 * registers its tag and function, so this list never names a module.
	 */
	private fun Function.foundBlock(ra: Ra) {
		raw("data modify storage ra:temp wrench_found set value 1b")

		for (route in ra.wrenchToggles) executeRun("if entity @s[tag=${route.tag}]", route.fn)

		// Multiblock bases - try to assemble or toggle existing multiblock.
		for (tag in BASE_TAGS) executeRun("if entity @s[tag=$tag] at @s", WrenchMultiblock.handleMultiblockBase)

		// No action for other blocks.
		val none = (ra.wrenchToggles.map { it.tag } + BASE_TAGS).joinToString(" ") { "unless entity @s[tag=$it]" }
		raw("""execute $none run tellraw @a[tag=ra.wrench_active,distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"This block cannot be toggled.",color:"gray"}]""")
	}
}
