package ra.modules.ender

import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.min
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
import ra.api.ModulesApi
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
import ra.core.block.stackedDataLine
import ra.core.block.stackedPropLine
import ra.core.define
import ra.core.raw
import ra.modules.ender.link.Link
import ra.core.item.loreLine
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.executeReturnRun
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.defaultProperty
import ra.core.block.finishPlacement
import ra.core.block.blockTick
import ra.core.block.BreakTest
import ra.core.block.markerSel
import ra.core.Tag
import ra.core.block.readProperty
import ra.core.executeStoreRun

/**
 * Ender Power Vault: a wireless bridge between two electric grids. It carries
 * `ra.wires.electric_node` so ra_wires:electric/adopt enrols it, and contributes no capacity: it
 * is a link, not a battery. What it sends comes out of the grid it is wired into and lands on the
 * grid at the far end, which is why two bases can share one set of batteries.
 *
 * `transfer_rate` is the only knob -- how much EU crosses per cycle.
 */
object PowerVault {
	val block = CustomBlock(Ns.ENDER, "ender_power_vault", "Ender Power Vault", model = "minecraft:purpur_pillar", dir = "power_vault")
	val item = block.enderItem(
		"rare",
		loreLine("Shares EU with the vault on its channel", "gray"),
		loreLine("Behaves as an electric node: wire into it", "gray"),
		loreLine("Shift+RMB with the wrench: link / send / receive", "dark_gray"),
	)

	val process = block.fn("process")
	private val level = block.fn("level")
	private val refund = block.fn("refund")
	val cycleMode = block.fn("cycle_mode")

	private const val T = Obj.TEMP

	fun Ra.registerPowerVault() {
		block(block)
		wrenchCycle(block.id, WrenchCycle("Mode", "mode", cycleMode))
		readonly(block.id, "transfer_rate")
		pack.blockRegisterMessage(block)
		pack.enderGive(block, item, """{text:" — wire into it like a switch",color:"gray"}""")
		pack.enderRecipe(block, item, RecipeCategory.REDSTONE) {
			pattern("CPC", "URU", "CUC")
			key("C", Items.COPPER_INGOT)
			key("P", Items.ENDER_PEARL)
			key("U", Items.PURPUR_PILLAR)
			key("R", Items.REDSTONE_BLOCK)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.properties.mode", "\"link\"")
			setOnNew(block, "data.properties.transfer_rate", "80")

			raw("tag ${block.markerSel("tag=${Tag.NEW}", "distance=..0.5")} add ${EnderIds.ELECTRIC_NODE}")

			finishPlacement(block)
		}

		pack.cycleLinkSendReceive(block)
		pack.define(process) { process() }
		pack.define(level) { level() }

		// Put back the part of a send the far side could not accept. As the sending vault marker.
		// Reads #ender.back.
		//
		// The offer can itself be refused -- the local grid may have filled up in the meantime,
		// though nothing between the take and here can do that. Whatever comes back unaccepted is
		// genuinely lost, and that is the only path in the vault where EU can disappear; it needs
		// both grids full at once to happen at all.
		pack.define(refund) {
			raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ender.back $T")
			function(ModulesApi.Wires.offerEu, Storage.WIRES, "eu")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.4)
			stackReset(top = 120, step = 16)
			stackedPropLine("channel", "Channel: ", "light_purple")
			stackedPropLine("mode", "Mode: ", "aqua")
			stackedPropLine("transfer_rate", "Rate: ", "yellow", suffix = " EU")
			stackedDataLine("eu", "Buffer: ", "green", suffix = " EU")
		}

		val onBreak = pack.enderOnBreak(block, item)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.PURPUR_PILLAR)) {
			// Defaults, including the electric-node tag a vault placed by an older build lacks.
			defaultProperty(block, "channel", "\"default\"")
			defaultProperty(block, "mode", "\"link\"")
			defaultProperty(block, "transfer_rate", "80")
			raw("tag ${block.markerSel()} add ${EnderIds.ELECTRIC_NODE}")

			raw("tag @e[type=marker,tag=${EnderIds.RECV_POWER}] remove ${EnderIds.RECV_POWER}")
			tagUnlessMode(block, "send", EnderIds.RECV_POWER)

			raw("scoreboard players add ${block.markerSel()} ${EnderIds.CD} 1")
			executeRun("as ${block.markerSel("scores={${EnderIds.CD}=5..}")} at @s", process)
		}
	}

	/**
	 * Move EU from this vault's grid to its partner's, over the channel. As a vault marker.
	 *
	 * It used to hold a private buffer and trade that buffer with its partner's, which meant it was
	 * not really wired into anything: charge had to seep into the vault under the old per-node
	 * model before there was anything to send, and once electric moved onto the network engine
	 * there was no seeping left to do.
	 *
	 * NOTHING IS CREATED OR DESTROYED HERE. The sender debits its grid only by what the receiver's
	 * grid actually accepted, and anything the far side could not take is put straight back. A
	 * full receiving grid costs nothing rather than voiding the difference.
	 */
	private fun Function.process() {
		scoreboard.players.set(self(), EnderIds.CD, 0)
		tag(self()) { add(EnderIds.SELF) }

		executeReturnRun("if data entity @s data.properties{mode:\"receive\"}", Link.done)

		// What this grid can spare.
		function(RaLib.Transport.Net.read)
		raw("scoreboard players operation #ender.eu $T = #net_amount ${Obj.TR_TMP}")
		readProperty("transfer_rate", default = 80, min = 1)
		raw("scoreboard players operation #ender.rate $T = #prop $T")
		executeReturnRun("if score #ender.eu $T matches ..0", Link.done)

		// One-way: up to the rate, whatever the far side holds.
		computeScore("#ender.carry", T, provider { min(scoreOf("#ender.eu", T), scoreOf("#ender.rate", T)) })

		// Two-way: half the gap between the two GRIDS, and only downhill.
		executeRun("if data entity @s data.properties{mode:\"link\"}", level)
		executeReturnRun("if score #ender.carry $T matches ..0", Link.done)

		// Take first, so the receiver cannot be handed EU that still exists here. The refund below
		// puts back anything it turns out not to want.
		raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ender.carry $T")
		executeStoreRun("", "#ender.carry", ModulesApi.Wires.takeEu, "with storage ra:wires eu")
		executeReturnRun("if score #ender.carry $T matches ..0", Link.done)

		raw("data modify storage ra:ender power set value {}")
		raw("data modify storage ra:ender power.channel set from entity @s data.properties.channel")
		function(Link.sendPower, Storage.ENDER, "power")

		// Refund whatever the partner's grid could not hold.
		computeScore("#ender.back", T, provider { sub(scoreOf("#ender.carry", T), scoreOf("#ender.sent", T)) })
		executeRun("if score #ender.back $T matches 1..", refund)

		raw("execute if score #ender.sent $T matches 1.. run particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 3")

		function(Link.done)
	}

	/** Half the gap between the two GRIDS, or nothing. Reads #ender.eu, writes #ender.carry. */
	private fun Function.level() {
		raw("scoreboard players set #ender.theirs $T -1")
		raw("data modify storage ra:ender probe set value {}")
		raw("data modify storage ra:ender probe.channel set from entity @s data.properties.channel")
		function(Link.probePower, Storage.ENDER, "probe")

		raw("execute if score #ender.theirs $T matches ..-1 run return run scoreboard players set #ender.carry $T 0")

		computeScore("#ender.gap", T, provider { sub(scoreOf("#ender.eu", T), scoreOf("#ender.theirs", T)) })
		raw("execute if score #ender.gap $T matches ..4 run return run scoreboard players set #ender.carry $T 0")

		// carry = min(gap / 2, rate); the gap is at least 5 here, so the floor is a plain halving.
		computeScore("#ender.carry", T, provider { min(floorDiv(scoreOf("#ender.gap", T), constant(2)), scoreOf("#ender.rate", T)) })
	}
}
