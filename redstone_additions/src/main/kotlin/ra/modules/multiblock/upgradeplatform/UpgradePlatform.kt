package ra.modules.multiblock.upgradeplatform

import ra.core.block.soundListenersSel
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.selector.Sort
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import ra.api.MultiblockLib
import ra.api.multiblock
import ra.api.multiblockSpec
import ra.api.pos
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.TagRoute
import ra.core.block.particleViewersSel
import ra.core.define
import ra.core.hook
import ra.core.executeRun
import ra.api.Goggles
import ra.core.raw
import ra.core.ref
import ra.modules.multiblock.forEachMarker

/**
 * Upgrade Platform (`ra_multiblock:upgrade_platform/...`): a 3x1x3 smooth stone platform around a
 * netherite base. Items dropped on it are upgraded one tier, with a chance of being destroyed.
 *
 * A compiled multiblock ([SPEC]). Rotationally symmetric: one predicate, and markers keep
 * `data.facing:"none"`, as they always had.
 */
object UpgradePlatform {
	private fun fn(path: String) = ref(Ns.MULTIBLOCK, "upgrade_platform/$path")

	const val ID = "upgrade_platform"

	/**
	 * Top view: S S S / S N S / S S S -- S = smooth stone, N = netherite base. The base itself is
	 * not part of the pattern; the base's own break detection covers it.
	 */
	val SPEC = multiblockSpec(ID, "Upgrade Platform", "netherite") {
		hint = "Upgrade Platform: 3x3 smooth stone platform with netherite base in center"
		rotates = false
		symmetricFacing = "none"
		box(-1..1, 0..0, -1..1, "minecraft:smooth_stone") { it == pos(0, 0, 0) }
	}

	/** `ra.multiblock.upgrade_platform`, given by the library's generated setup_type. */
	val TAG = SPEC.tag

	/** On a dropped item whose upgrade roll failed, for the rest of try_upgrade. */
	private const val FAILED = "ra.upgrade_failed"

	val tickDispatch = fn("tick_dispatch")
	val tick = fn("tick")
	val tryUpgrade = fn("try_upgrade")
	val goggles = fn("goggles")

	fun Ra.registerUpgradePlatform() {
		multiblock(SPEC)

		// No on_break: the platform has no containers.
		pack.define(tickDispatch) { forEachMarker(TAG, tick) }
		pack.define(tick) { tick() }
		pack.define(tryUpgrade) { tryUpgrade() }

		// Name only: no IO ports, no status. One billboard line, 0.3 above the 1-tall platform.
		pack.define(UpgradePlatform.goggles) {
			raw("""data modify storage ra:temp gg.line set value {text:"Upgrade Platform",color:"green",bold:true}""")
			function(Goggles.Billboard.append)
			executeRun("positioned ~ ~1.3 ~", Goggles.Billboard.flushHere)
		}

		with(UpgradeRecipes) { registerRecipes() }

		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
		multiblockGoggles += TagRoute(TAG, UpgradePlatform.goggles)
	}

	/** As the marker, at the base: picks up an item dropped on the platform and upgrades it. */
	private fun Function.tick() {
		// Ambient particles, always.
		raw("particle minecraft:enchant ~ ~1.5 ~ 1 0.5 1 1 3 normal ${particleViewersSel()}")

		// A redstone block under the centre locks the platform.
		raw("execute if block ~ ~-1 ~ minecraft:redstone_block run return 0")

		// Every 40 ticks (2 s).
		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute unless score @s ${Obj.COOLDOWN} matches 40.. run return 0")
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)

		// One dropped item in the 3x1x3 box just above the surface: (~-1, ~0.5, ~-1) to (~1, ~1.5, ~1).
		execute {
			positioned(vec3(-1.0, 0.5, -1.0).relative)
			asTarget(allEntities(limitToOne = true) {
				type = EntityTypes.ITEM
				dx = 2.0; dy = 1.0; dz = 2.0
				sort = Sort.NEAREST
			})
			run { function(tryUpgrade) }
		}
	}

	/** As the dropped item, on the platform. */
	private fun Function.tryUpgrade() {
		val t = Obj.TEMP
		data(Storage.TEMP) { remove("upgrade") }
		data(Storage.TEMP) { modify("upgrade.input") { set(self(), "Item") } }

		data(Storage.TEMP) { remove("upgrade.result") }
		function(UpgradeRecipes.matchAll)
		raw("execute unless data storage ra:temp upgrade.result run return 0")

		// Roll this recipe's success chance (1..100).
		raw("execute store result score #up_roll $t run random roll 1..100")
		raw("execute store result score #up_chance $t run data get storage ra:temp upgrade.chance 1")

		// A failed roll destroys the item without producing anything.
		raw("execute unless score #up_roll $t <= #up_chance $t run tag @s add $FAILED")

		// Consume one from the input stack.
		raw("execute store result score #up_count $t run data get entity @s Item.count")
		scoreboard.players.remove(literal("#up_count"), t, 1)
		raw("execute if score #up_count $t matches 0 run kill @s")
		raw("execute if score #up_count $t matches 1.. store result entity @s Item.count int 1 run scoreboard players get #up_count $t")

		// Failed: break feedback, and stop.
		raw("execute if entity @s[tag=$FAILED] at @s run particle minecraft:smoke ~ ~0.5 ~ 0.15 0.15 0.15 0.01 10 normal ${particleViewersSel()}")
		raw("execute if entity @s[tag=$FAILED] at @s run playsound minecraft:block.grindstone.use block ${soundListenersSel(16)} ~ ~ ~ 0.7 0.7")
		tag(self()) { remove(FAILED) }
		raw("execute if score #up_roll $t > #up_chance $t run return 0")

		// Spawn the upgraded item.
		raw("""execute at @s run summon item ~ ~0.5 ~ {Item:{id:"minecraft:stone",count:1},Tags:["ra","ra.upgraded"],PickupDelay:20}""")
		raw("execute as @e[type=item,tag=ra.upgraded,distance=..1.0,limit=1,sort=nearest] run data modify entity @s Item set from storage ra:temp upgrade.result")
		raw("execute as @e[type=item,tag=ra.upgraded,distance=..1.0,limit=1,sort=nearest] run tag @s remove ra.upgraded")

		raw("execute at @s run particle minecraft:totem_of_undying ~ ~0.5 ~ 0.2 0.3 0.2 0.1 15 normal ${particleViewersSel()}")
		raw("execute at @s run particle minecraft:electric_spark ~ ~0.5 ~ 0.3 0.3 0.3 0.05 8 normal ${particleViewersSel()}")
		raw("execute at @s run playsound minecraft:block.amethyst_block.chime block ${soundListenersSel(16)} ~ ~ ~ 1 1.2")
		raw("execute at @s run playsound minecraft:block.enchantment_table.use block ${soundListenersSel(16)} ~ ~ ~ 0.5 1.5")
	}
}
