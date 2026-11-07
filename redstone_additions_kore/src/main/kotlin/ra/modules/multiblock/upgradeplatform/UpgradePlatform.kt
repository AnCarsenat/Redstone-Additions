package ra.modules.multiblock.upgradeplatform

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.core.Storage
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.selector.Sort
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import ra.api.MultiblockLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TagRoute
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.core.ref

/**
 * Upgrade Platform (`ra_multiblock:upgrade_platform/...`): a 3x1x3 smooth stone platform around a
 * netherite base. Items dropped on it are upgraded one tier, with a chance of being destroyed.
 * Rotationally symmetric, so it needs no direction data.
 */
object UpgradePlatform {
	private fun fn(path: String) = ref(Ns.MULTIBLOCK, "upgrade_platform/$path")

	const val TAG = "ra.multiblock.upgrade_platform"

	/** On a dropped item whose upgrade roll failed, for the rest of try_upgrade. */
	private const val FAILED = "ra.upgrade_failed"

	val checkBlocks = fn("check_blocks")
	val checkStructure = fn("check_structure")
	val validate = fn("validate")
	val setupType = fn("setup_type")
	val onBreak = fn("on_break")
	val tickDispatch = fn("tick_dispatch")
	val tick = fn("tick")
	val tryUpgrade = fn("try_upgrade")
	val goggles = fn("goggles")

	fun Ra.registerUpgradePlatform() {
		// Structure (top view): S S S / S N S / S S S -- S = smooth stone, N = netherite base.
		// At the base. Returns 1 if valid, 0 otherwise. No side effects.
		pack.define(checkBlocks) {
			for ((x, z) in listOf(-1 to -1, 0 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 0 to 1, 1 to 1))
				raw("execute unless block ~$x ~ ~$z minecraft:smooth_stone run return 0")
			returnValue(1)
		}

		// Periodic integrity check. As the marker. Sets @s ra.multiblock to 1 (intact) or 0.
		pack.define(checkStructure) {
			raw("execute unless entity @s[tag=$TAG] run return 0")
			execute {
				storeResult { score(self(), Obj.MULTIBLOCK) }
				run { function(checkBlocks) }
			}
		}

		// Assembly validation. Sets #mb_result ra.multiblock to 1 if valid.
		pack.define(validate) {
			raw("""execute unless data storage ra:multiblock {type:"upgrade_platform"} run return 0""")
			execute {
				storeResult { score(literal("#mb_result"), Obj.MULTIBLOCK) }
				run { function(checkBlocks) }
			}
			// Symmetric: no facing to store.
			raw("""execute if score #mb_result ${Obj.MULTIBLOCK} matches 1 run data modify storage ra:multiblock facing set value "none"""")
		}

		pack.define(setupType) {
			raw("""execute if data entity @s data{type:"upgrade_platform"} run tag @s add $TAG""")
		}
		pack.define(onBreak) {
			raw("execute unless entity @s[tag=$TAG] run return 0")
			// No cleanup needed: the platform has no containers.
		}
		pack.define(tickDispatch) {
			execute {
				asTarget(allEntities { type = EntityTypes.MARKER; tag = TAG })
				at(self())
				run { function(tick) }
			}
		}
		pack.define(tick) { tick() }
		pack.define(tryUpgrade) { tryUpgrade() }

		// Name only: no IO ports, no status. The platform is 1 block tall, so y+1.5 is well above.
		pack.define(UpgradePlatform.goggles) {
			raw("""summon text_display ~ ~1.5 ~ {Tags:["ra","ra.billboard","ra.display"],billboard:"center",text:{text:"Upgrade Platform",color:"green",bold:true},background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.6f,0.6f,0.6f]}}""")
		}

		with(UpgradeRecipes) { registerRecipes() }

		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.VALIDATE, validate)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.SETUP_TYPE, setupType)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.CHECK_STRUCTURE, checkStructure)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.ON_BREAK, onBreak)
		multiblockGoggles += TagRoute(TAG, UpgradePlatform.goggles)
	}

	/** As the marker, at the base: picks up an item dropped on the platform and upgrades it. */
	private fun Function.tick() {
		raw("execute unless data entity @s data.properties{enabled:1b} run return 0")

		// Ambient particles, whenever enabled.
		raw("particle minecraft:enchant ~ ~1.5 ~ 1 0.5 1 1 3 normal @a[scores={ra.u.par=1..}]")

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
		raw("execute if entity @s[tag=$FAILED] at @s run particle minecraft:smoke ~ ~0.5 ~ 0.15 0.15 0.15 0.01 10")
		raw("execute if entity @s[tag=$FAILED] at @s run playsound minecraft:block.grindstone.use block @a[distance=..16] ~ ~ ~ 0.7 0.7")
		tag(self()) { remove(FAILED) }
		raw("execute if score #up_roll $t > #up_chance $t run return 0")

		// Spawn the upgraded item.
		raw("""execute at @s run summon item ~ ~0.5 ~ {Item:{id:"minecraft:stone",count:1},Tags:["ra","ra.upgraded"],PickupDelay:20}""")
		raw("execute as @e[type=item,tag=ra.upgraded,distance=..1.0,limit=1,sort=nearest] run data modify entity @s Item set from storage ra:temp upgrade.result")
		raw("execute as @e[type=item,tag=ra.upgraded,distance=..1.0,limit=1,sort=nearest] run tag @s remove ra.upgraded")

		raw("execute at @s run particle minecraft:totem_of_undying ~ ~0.5 ~ 0.2 0.3 0.2 0.1 15")
		raw("execute at @s run particle minecraft:electric_spark ~ ~0.5 ~ 0.3 0.3 0.3 0.05 8")
		raw("execute at @s run playsound minecraft:block.amethyst_block.chime block @a[distance=..16] ~ ~ ~ 1 1.2")
		raw("execute at @s run playsound minecraft:block.enchantment_table.use block @a[distance=..16] ~ ~ ~ 0.5 1.5")
	}
}
