package ra.modules.multiblock

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.MultiblockLib
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.modules.multiblock.blastforge.BlastForge.registerBlastForge
import ra.modules.multiblock.blastforge.BlastForgeData.writeDirections
import ra.modules.multiblock.blocks.BaseTier
import ra.modules.multiblock.blocks.Bases
import ra.modules.multiblock.blocks.Bases.registerBases
import ra.modules.multiblock.drills.RockMetallicDrill.registerRockMetallicDrill
import ra.modules.multiblock.goggles.ShowIoLabel
import ra.modules.multiblock.upgradeplatform.UpgradePlatform.registerUpgradePlatform

/**
 * Multiblocks (`ra_multiblock`): the five Multiblock Base tiers and the structures assembled on
 * them (Blast Forge, Upgrade Platform, Rock Metallic Drill). The assembly machinery itself is
 * ra_lib_multiblock; this module plugs into it through the #ra_lib_multiblock:* hooks.
 */
object MultiblockModule : GameplayModule {
	override val ns = Ns.MULTIBLOCK
	override val title = "Multiblocks"

	override fun Ra.register() {
		registerBases()
		ShowIoLabel.register(pack)
		// Hook order inside each #ra_lib_multiblock tag: Blast Forge, Upgrade Platform, Drill.
		registerBlastForge()
		registerUpgradePlatform()
		registerRockMetallicDrill()

		// The Rock Metallic Drill is a compiled spec (registered in registerRockMetallicDrill).
		// Blast Forge and Upgrade Platform still use their own validators registered directly in
		// the #ra_lib_multiblock tags.

		pack.define(load) {
			raw("""tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Multiblock module loaded",color:"gray"}]""")
			function(Bases.registerAll)

			// Heat, for the Blast Forge.
			scoreboard.objectives.add(Obj.HEAT)

			writeDirections()

			// Tier data, the macro arguments of the base's give / placement / break.
			for (t in BaseTier.all) raw("data modify storage ra:multiblock ${t.path} set value ${t.tierDataSnbt()}")

			// The Upgrade Platform is rotationally symmetric: no direction data.
		}

		pack.define(tick!!) {
			// Break detection for every base tier: one check, which dispatches the cleanup macro.
			forEachMarker(Bases.BASE_TAG, Bases.checkBreak)

			// Periodic structure validation, every 40 ticks (2 s).
			val timer = literal("#mb_validate_timer")
			scoreboard.players.add(timer, Obj.MULTIBLOCK, 1)
			execute {
				ifCondition { score(timer, Obj.MULTIBLOCK, rangeOrIntStart(40)) }
				run { function(MultiblockLib.validateAll) }
			}
			execute {
				ifCondition { score(timer, Obj.MULTIBLOCK, rangeOrIntStart(40)) }
				run { scoreboard.players.set(timer, Obj.MULTIBLOCK, 0) }
			}

			// Active multiblocks, through the function tag: each type selects its own markers.
			function(Ns.LIB_MULTIBLOCK.id, MultiblockLib.Hooks.TICK, group = true)
		}

		bundles += GiveBundle("multiblock", "Multiblock Bases Bundle", "5 multiblock base tiers", ns, BaseTier.all.map { it.itemStackNbt() })
	}
}
