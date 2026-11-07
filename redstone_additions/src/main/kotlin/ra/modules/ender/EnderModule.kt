package ra.modules.ender

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.ender.FluidVault.registerFluidVault
import ra.modules.ender.ItemVault.registerItemVault
import ra.modules.ender.PowerVault.registerPowerVault
import ra.modules.ender.TeleportAnchor.registerTeleportAnchor
import ra.modules.ender.debug.EnderDebug.registerEnderDebug
import ra.modules.ender.link.Link.registerLink
import ra.modules.ender.tools.Anchor.registerAnchorTools
import ra.core.moduleLoaded
import ra.core.executeRun
import ra.core.block.markerSel

/**
 * Ender Links (`ra_ender`): remote vaults (items, fluid, EU) and teleport anchors.
 * See [EnderIds] for how a link works.
 */
object EnderModule : GameplayModule {
	override val ns = Ns.ENDER
	override val title = "Ender Links"

	override fun Ra.register() {
		// Registration order is the placement-handler order of the beet #ra:placement_handlers.
		registerItemVault()
		registerFluidVault()
		registerPowerVault()
		registerTeleportAnchor()
		registerLink()
		registerAnchorTools()
		registerEnderDebug()

		val blocks = listOf(ItemVault.block, FluidVault.block, PowerVault.block, TeleportAnchor.block)

		pack.define(load) {
			scoreboard.objectives.add(EnderIds.CD)
			// Teleport cooldowns: on the anchor, and on the player who just arrived.
			scoreboard.objectives.add(EnderIds.TP_CD)
			scoreboard.objectives.add(EnderIds.GRACE)

			for (b in blocks) function(b.fn("register_block"))

			moduleLoaded("Ender links loaded!", "light_purple")
		}

		pack.define(tick!!) {
			// Same reason as the anchor cooldown: a player with no grace score at all would never
			// match the selector that looks for one at or below zero. Seeded before the anchors run,
			// not after: a player who has never teleported has no score at all until this line
			// gives them one.
			raw("scoreboard players add @a ${EnderIds.GRACE} 0")

			// Each block's tick opens with several @e scans over its own markers, so a world with
			// none of them still paid for the scans. One existence check each is cheaper than the
			// four or five selectors behind it.
			for (b in blocks) executeRun("if entity ${b.markerSel("limit=1")}", b.tick)

			// Arrival grace, so a player landing on an anchor that is itself powered is not bounced
			// straight back.
			raw("execute if entity @a[scores={${EnderIds.GRACE}=1..}] run scoreboard players remove @a[scores={${EnderIds.GRACE}=1..}] ${EnderIds.GRACE} 1")
		}

		// Give every RA Ender block.
		pack.define(ref(ns, "items/give_all")) { for (b in blocks) function(b.give) }

		bundles += GiveBundle(
			"ender", "Ender Bundle", "Remote vaults and teleport anchors", ns,
			listOf(ItemVault.item, FluidVault.item, PowerVault.item, TeleportAnchor.item).map { it.stackNbt() },
		)
	}
}
