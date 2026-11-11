package ra.modules.wires.blocks.tesla_coil

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.features.tags.entityTypeTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.utils.toSnbt
import ra.api.RaLib
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.WrenchCycle
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.particleViewersSel
import ra.core.block.propLine
import ra.core.block.readProperty
import ra.core.block.soundListenersSel
import ra.core.computeScore
import ra.core.define
import ra.core.executeRun
import ra.core.provider
import ra.core.raw
import ra.core.ref
import ra.core.scoreOf
import ra.modules.wires.WiresBlocks
import ra.modules.wires.WiresBlocks.stackNbt
import ra.modules.wires.common.Common
import ra.modules.wires.common.line
import ra.modules.wires.electric.EuApi
import ra.modules.wires.machine.MachineClock
import ra.core.block.WRENCH_READOUT

/**
 * Tesla Coil: while powered by redstone, once a second, zaps every mob of
 * `#ra_wires:tesla_targets` within `radius` for `damage` (lightning damage), paying `eu_per_zap`
 * per mob hit to the EU grid it is on.
 *
 * NO COST WITHOUT A TARGET. The first thing a run does past the redstone test is one selector
 * bounded by the radius with `limit=1`. Only when it finds something does the coil read its grid.
 *
 * ALL OR NOTHING, PER MOB. The grid is read once, `floor(EU / eu_per_zap)` is how many mobs it
 * can pay for, and that (at most [MAX_TARGETS]) is the selector's `limit`, nearest first. The
 * mobs actually hit are paid for in one take afterwards, which the read guarantees is covered:
 * a mob is never zapped for free, and EU is never taken for a zap that did not happen.
 *
 * TARGETS ARE A TAG, NOT `type=!player`. Hostile mobs only, so pets, villagers, golems, armour
 * stands and the pack's own entities are never hit. Another pack adds a mob to
 * `#ra_wires:tesla_targets` to make it a target.
 */
object TeslaCoil : PackPart {
	const val ID = "tesla_coil"

	/** Waxed: an unwaxed rod oxidises into another block id, which the break check reads as broken. */
	const val BLOCK = "minecraft:waxed_lightning_rod"

	const val DEFAULT_RADIUS = 8
	const val MAX_RADIUS = 16
	/** A selector's sphere grows with the cube of its radius. */
	const val ADMIN_MAX_RADIUS = 32
	const val DEFAULT_DAMAGE = 6
	const val MAX_DAMAGE = 40
	const val DEFAULT_EU = 200
	const val MAX_EU = 100_000
	/** Mobs hit per zap at most: bounds the arcs drawn in one tick. */
	const val MAX_TARGETS = 16

	private const val TARGETS = "#ra_wires:tesla_targets"
	private const val T = "ra.wires.tmp"

	val goggles = ref(Ns.WIRES, "blocks/$ID/goggles")
	val run = ref(Ns.WIRES, "blocks/$ID/run")
	val onBreak = ref(Ns.WIRES, "blocks/$ID/on_break")
	private val scan = ref(Ns.WIRES, "blocks/$ID/scan")
	private val zap = ref(Ns.WIRES, "blocks/$ID/zap")
	private val hit = ref(Ns.WIRES, "blocks/$ID/hit")
	private val arc = ref(Ns.WIRES, "blocks/$ID/arc")
	val cycleRadius = ref(Ns.WIRES, "blocks/$ID/cycle_radius")

	override fun Ra.register() {
		properties(ID) {
			number("radius", min = 1, max = MAX_RADIUS, adminMax = ADMIN_MAX_RADIUS, default = DEFAULT_RADIUS)
			// Balance, so admin only; the default for new coils is on the Power & Fluids page.
			number("damage", min = 1, max = MAX_DAMAGE, adminOnly = true, default = DEFAULT_DAMAGE)
			number("eu_per_zap", min = 1, max = MAX_EU, adminOnly = true, default = DEFAULT_EU)
		}
		wrenchCycle(ID, WrenchCycle("Radius", "radius", cycleRadius))

		// Shift+RMB with the Wrench: 4, 8, 12, 16 and round; anything else (an admin's radius)
		// goes back to 4. As the marker, at it.
		pack.define(cycleRadius) {
			readProperty("radius", default = DEFAULT_RADIUS, min = 1)
			raw("scoreboard players set #tc.r $T 4")
			for (r in listOf(4, 8, 12)) raw("execute if score #prop ra.temp matches $r run scoreboard players set #tc.r $T ${r + 4}")
			raw("execute store result entity @s data.properties.radius int 1 run scoreboard players get #tc.r $T")
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"Tesla Coil radius: ",color:"gray"},{nbt:"data.properties.radius",entity:"@s",color:"aqua"},{text:" blocks",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.entityTypeTag("tesla_targets", Ns.WIRES.id) {
			for (e in listOf(
				EntityTypes.BLAZE, EntityTypes.BOGGED, EntityTypes.BREEZE, EntityTypes.CAVE_SPIDER,
				EntityTypes.CREEPER, EntityTypes.DROWNED, EntityTypes.ELDER_GUARDIAN, EntityTypes.ENDERMITE,
				EntityTypes.EVOKER, EntityTypes.GHAST, EntityTypes.GUARDIAN, EntityTypes.HOGLIN,
				EntityTypes.HUSK, EntityTypes.ILLUSIONER, EntityTypes.MAGMA_CUBE, EntityTypes.PARCHED,
				EntityTypes.PHANTOM, EntityTypes.PIGLIN_BRUTE, EntityTypes.PILLAGER, EntityTypes.RAVAGER,
				EntityTypes.SHULKER, EntityTypes.SILVERFISH, EntityTypes.SKELETON, EntityTypes.SLIME,
				EntityTypes.SPIDER, EntityTypes.STRAY, EntityTypes.VEX, EntityTypes.VINDICATOR,
				EntityTypes.WARDEN, EntityTypes.WITCH, EntityTypes.WITHER_SKELETON, EntityTypes.ZOGLIN,
				EntityTypes.ZOMBIE, EntityTypes.ZOMBIE_VILLAGER,
			)) add(e)
		}

		pack.define(TeslaCoil.run) { run() }
		pack.define(scan) { scan() }

		// Macro {r, n, d}: numbers read just before with readProperty / the grid read.
		pack.define(zap) {
			raw("scoreboard players set #tc.hit $T 0")
			raw("\$execute as @e[type=$TARGETS,distance=..\$(r),limit=\$(n),sort=nearest] run function ${hit.asId()} {d:\$(d)}")
		}

		// Macro {d}. As the mob, at the coil.
		pack.define(hit) {
			raw("\$damage @s \$(d) minecraft:lightning_bolt")
			raw("scoreboard players add #tc.hit $T 1")
			raw("scoreboard players set #tc.step $T 0")
			executeRun("if entity ${particleViewersSel(48)} positioned ~ ~0.8 ~ facing entity @s eyes", arc)
		}

		// As the mob, stepping from the coil's tip towards it, half a block a step, until the
		// point is inside the mob's hitbox (`dx=0` is the 1x1x1 box at the point) or the
		// step cap (the farthest a target can be, admin radius included) is reached.
		pack.define(arc) {
			raw("particle minecraft:electric_spark ~ ~ ~ 0.04 0.04 0.04 0 2 normal ${particleViewersSel()}")
			raw("scoreboard players add #tc.step $T 1")
			raw("execute if score #tc.step $T matches ${ADMIN_MAX_RADIUS * 2 + 4}.. run return 0")
			raw("execute if entity @s[dx=0,dy=0,dz=0] run return 0")
			raw("execute positioned ^ ^ ^0.5 run function ${arc.asId()}")
		}

		pack.define(TeslaCoil.goggles) {
			gogglesHeader("Tesla Coil", nameY = 1.0)
			dataLine("coil", "Coil: ", "yellow", y = 0.8)
			propLine("radius", "Radius: ", "aqua", y = 0.6)
			propLine("eu_per_zap", "Per mob: ", "aqua", y = 0.4, suffix = " EU")
			dataLine("available_eu", "EU: ", "aqua", y = 0.2, suffix = " EU")
		}

		pack.define(TeslaCoil.onBreak) {
			// As the marker, at it. Ends by killing the marker.
			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			tag(self()) { remove(MachineClock.TAG) }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop(BLOCK)
			raw("summon item ~ ~ ~ {Item:${WiresBlocks.items.first { it.block.id == ID }.stackNbt().toSnbt()}}")
			kill(self())
		}
	}

	/** As the marker, at it, once a second from the machine clock. */
	private fun Function.run() {
		line("execute unless function", RaLib.Redstone.any, "run return run data modify entity @s data.status.coil set value \"No redstone\"")
		raw("execute unless score @s ra.tr.net matches 1.. run return run data modify entity @s data.status.coil set value \"No grid\"")
		readCappedProperty("radius", default = DEFAULT_RADIUS, min = 1, adminMax = ADMIN_MAX_RADIUS)
		raw("data remove storage ra:wires tc")
		raw("execute store result storage ra:wires tc.r int 1 run scoreboard players get #prop ra.temp")
		function(scan, Storage.WIRES, "tc")
		raw("data remove storage ra:wires tc")
	}

	/** Macro {r}. As the marker, at it. */
	private fun Function.scan() {
		raw("\$execute unless entity @e[type=$TARGETS,distance=..\$(r),limit=1] run return run data modify entity @s data.status.coil set value \"No targets\"")

		readProperty("eu_per_zap", default = DEFAULT_EU, min = 1)
		raw("scoreboard players operation #tc.cost $T = #prop ra.temp")
		function(RaLib.Transport.Net.read)
		raw("execute if score #net_amount ra.tr.tmp < #tc.cost $T run return run data modify entity @s data.status.coil set value \"No EU\"")

		computeScore("#tc.n", T, provider { min(floorDiv(scoreOf("#net_amount", "ra.tr.tmp"), scoreOf("#tc.cost", T)), constant(MAX_TARGETS)) },
			"store result storage ra:wires tc.n int 1")
		readProperty("damage", default = DEFAULT_DAMAGE, min = 1)
		raw("execute if score #prop ra.temp matches ${MAX_DAMAGE + 1}.. run scoreboard players set #prop ra.temp $MAX_DAMAGE")
		raw("execute store result storage ra:wires tc.d int 1 run scoreboard players get #prop ra.temp")
		function(zap, Storage.WIRES, "tc")

		// Pay for the mobs hit, in one take: n was what the grid could cover, so this is covered.
		computeScore("#tc.pay", T, provider { mul(scoreOf("#tc.hit", T), scoreOf("#tc.cost", T)) },
			"store result storage ra:wires eu.amount int 1")
		executeRun("if score #tc.pay $T matches 1..", EuApi.takeEu, "with storage ra:wires eu")

		raw("data modify entity @s data.status.coil set value \"Zapping\"")
		raw("execute store result entity @s data.status.last_hits int 1 run scoreboard players get #tc.hit $T")
		raw("particle minecraft:electric_spark ~ ~0.8 ~ 0.15 0.3 0.15 0.2 12 normal ${particleViewersSel()}")
		raw("playsound minecraft:entity.lightning_bolt.impact block ${soundListenersSel(24)} ~ ~ ~ 0.5 1.6")
	}
}
