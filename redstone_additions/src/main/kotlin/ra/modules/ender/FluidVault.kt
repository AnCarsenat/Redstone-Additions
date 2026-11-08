package ra.modules.ender

import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.sub
import ra.core.computeScore
import ra.core.provider
import ra.core.scoreOf
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.stackReset
import ra.core.block.stackedPropLine
import ra.core.define
import ra.core.raw
import ra.modules.ender.link.Link
import ra.core.item.loreLine
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.executeReturnRun
import ra.core.block.newMarkerSel
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.defaultProperty
import ra.core.block.finishPlacement
import ra.core.block.blockTick
import ra.core.block.BreakTest
import ra.core.block.markerSel
import ra.core.Tag
import ra.core.block.readProperty

/**
 * Ender Fluid Vault: an ordinary fluid node with a small buffer, so pipes, pumps and tanks
 * connect to it with no special case. What it adds is the link: whatever reaches its network can
 * be pushed to the network of the vault on its channel.
 */
object FluidVault {
	val block = CustomBlock(Ns.ENDER, "ender_fluid_vault", "Ender Fluid Vault", model = "minecraft:purpur_block", dir = "fluid_vault")
	val item = block.enderItem(
		"rare",
		loreLine("Shares liquid and gas with the vault on its channel", "gray"),
		loreLine("Joins the fluid network like a tank", "gray"),
		loreLine("Shift+RMB with the wrench: link / send / receive", "dark_gray"),
	)

	val process = block.fn("process")
	private val level = block.fn("level")
	val cycleMode = block.fn("cycle_mode")

	private const val T = Obj.TEMP

	fun Ra.registerFluidVault() {
		block(block)
		wrenchCycle(block.id, WrenchCycle("Mode", "mode", cycleMode))
		readonly(block.id, "transfer_rate")
		pack.blockRegisterMessage(block)
		pack.enderGive(block, item, """{text:" — pipe into it like a tank",color:"gray"}""")
		pack.enderRecipe(block, item, RecipeCategory.MISC) {
			pattern("CPC", "UKU", "CUC")
			key("C", Items.COPPER_INGOT)
			key("P", Items.ENDER_PEARL)
			key("U", Items.PURPUR_BLOCK)
			key("K", Items.BUCKET)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.properties.mode", "\"link\"")
			setOnNew(block, "data.properties.transfer_rate", "200")

			// Fluid network membership, and the fluid flag the wires status readouts look for.
			raw("tag ${block.markerSel("tag=${Tag.NEW}", "distance=..0.5")} add ${EnderIds.FLUID_NODE}")
			executeRun("as ${block.newMarkerSel()}", RaLib.Transport.Net.join, "{class:\"fluid\",capacity:1000}")

			finishPlacement(block)
		}

		pack.cycleLinkSendReceive(block)
		pack.define(process) { process() }
		pack.define(level) { level() }
		pack.define(block.goggles) { goggles() }

		// The node has to leave its network before the marker dies, or the network's contents go
		// with it.
		val onBreak = pack.enderOnBreak(block, item) { function(RaLib.Transport.Net.leave) }

		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.PURPUR_BLOCK)) {
			// Defaults, and a node that lost its network membership on reload rejoins.
			defaultProperty(block, "channel", "\"default\"")
			defaultProperty(block, "mode", "\"link\"")
			defaultProperty(block, "transfer_rate", "200")
			executeRun("as ${block.markerSel("tag=!ra.tr.node")}", RaLib.Transport.Net.rejoin, "{class:\"fluid\"}")

			raw("tag @e[type=marker,tag=${EnderIds.RECV_FLUID}] remove ${EnderIds.RECV_FLUID}")
			tagUnlessMode(block, "send", EnderIds.RECV_FLUID)

			// Every 10 ticks, so a link is a steady trickle rather than a spike.
			raw("scoreboard players add ${block.markerSel()} ${EnderIds.CD} 1")
			executeRun("as ${block.markerSel("scores={${EnderIds.CD}=10..}")} at @s", process)
		}
	}

	/**
	 * Move a slice of fluid between this network and the partner's. As a vault marker, at the block.
	 *
	 * Two-way here is easier than for items, because fluid is not made of stacks: a pair of linked
	 * networks can simply find their level. Only the fuller side ever pushes, and it pushes half the
	 * difference, so the two converge and neither ever hands the same fluid back. A dead zone stops
	 * the last few units bouncing.
	 *
	 * Take first, then offer, then hand back whatever the far side would not accept. In that order
	 * the fluid is only ever in one network, and a full receiver or a medium mismatch costs nothing.
	 */
	private fun Function.process() {
		scoreboard.players.set(self(), EnderIds.CD, 0)
		tag(self()) { add(EnderIds.SELF) }

		executeReturnRun("if data entity @s data.properties{mode:\"receive\"}", Link.done)

		function(RaLib.Transport.Net.read)
		executeReturnRun("if score #net_amount ${Obj.TR_TMP} matches ..0", Link.done)
		executeReturnRun("unless data storage ra:transport cur.medium", Link.done)

		raw("scoreboard players operation #ender.mine $T = #net_amount ${Obj.TR_TMP}")
		readProperty("transfer_rate", default = 200, min = 1)
		raw("scoreboard players operation #ender.rate $T = #prop $T")

		raw("data modify storage ra:ender fluid set value {}")
		raw("data modify storage ra:ender fluid.medium set from storage ra:transport cur.medium")
		raw("data modify storage ra:ender fluid.channel set from entity @s data.properties.channel")

		// A one-way sender moves up to its rate and does not care what the far side holds.
		raw("scoreboard players operation #ender.want $T = #ender.rate $T")

		// A two-way link moves half the gap instead, and only when it is the fuller side.
		executeRun("if data entity @s data.properties{mode:\"link\"}", level)

		executeReturnRun("if score #ender.want $T matches ..0", Link.done)
		raw("execute if score #ender.want $T > #ender.rate $T run scoreboard players operation #ender.want $T = #ender.rate $T")
		raw("execute store result storage ra:ender fluid.rate int 1 run scoreboard players get #ender.want $T")

		function(Link.sendFluid, Storage.ENDER, "fluid")

		function(Link.done)
	}

	/**
	 * How much a two-way link should move: half the gap, or nothing.
	 * As the sending vault marker. Reads #ender.mine, writes #ender.want.
	 */
	private fun Function.level() {
		// What the partner's network holds. -1 means there is no partner.
		raw("scoreboard players set #ender.theirs $T -1")
		raw("data modify storage ra:ender probe set value {}")
		raw("data modify storage ra:ender probe.channel set from entity @s data.properties.channel")
		function(Link.probeFluid, Storage.ENDER, "probe")

		raw("execute if score #ender.theirs $T matches ..-1 run return run scoreboard players set #ender.want $T 0")

		computeScore("#ender.gap", T, provider { sub(scoreOf("#ender.mine", T), scoreOf("#ender.theirs", T)) })

		// Dead zone: below this the pair is level enough, and pushing would just be noise.
		raw("execute if score #ender.gap $T matches ..20 run return run scoreboard players set #ender.want $T 0")

		// Half the gap, floored as scoreboard `/=` did. process() clamps it to the rate.
		computeScore("#ender.want", T, provider { floorDiv(scoreOf("#ender.gap", T), constant(2)) })
	}

	private fun Function.goggles() {
		gogglesHeader(block, nameY = 1.4)
		stackReset(top = 120, step = 16)
		stackedPropLine("channel", "Channel: ", "light_purple")
		stackedPropLine("mode", "Mode: ", "aqua")
		stackedPropLine("transfer_rate", "Rate: ", "yellow", suffix = "/s")

		// What the local network holds, read live rather than stored on the marker.
		function(RaLib.Transport.Net.read)
		raw("execute store result score #ender.amount $T run scoreboard players get #net_amount ${Obj.TR_TMP}")
		raw("data modify storage ra:temp ender_line set value {label:\"Holding: \",value:\"empty\",color:\"gray\",suffix:\"\"}")
		raw("execute if data storage ra:transport cur.medium run data modify storage ra:temp ender_line.value set from storage ra:transport cur.medium")
		raw("execute if data storage ra:transport cur.medium run data modify storage ra:temp ender_line.color set value \"aqua\"")
		stackedTextLineFromStorage()
	}
}
