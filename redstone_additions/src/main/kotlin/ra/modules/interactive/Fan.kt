package ra.modules.interactive

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.tags.entityTypeTag
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.breakCheck
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.stackedPropLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw

/**
 * Fan: while powered, pushes items, mobs and players along its face, through the `range` blocks in
 * front of it, at a speed set by `strength`. A Conveyor with a reach.
 *
 * A DROPPER, FOR ITS `triggered` STATE. The vanilla block already knows whether it is powered, so
 * the Fan asks nothing of ra_lib:redstone (see ra_lib:redstone/detect, "CHEAPER STILL"); a dropper
 * also has the full six-way facing a Fan needs, placed like the Spitter's. Its inventory is unused.
 *
 * COST. Every other tick, one test per Fan of its own block state; an unpowered Fan stops there.
 * A powered one reads its two properties and runs two box selectors over the `range` blocks in
 * front of it (see [FanPush]). The only per-tick work is the break check every block has, and one
 * `@a` tag filter for players still being lifted by an upward Fan.
 */
object Fan {
	val block = CustomBlock(Ns.INTERACTIVE, "fan", "Fan", model = "minecraft:dropper", dirType = 2)
	val item = block.loreItem { lore(loreLine("Pushes what is in front of it while powered", "gray")) }
	val registerBlock = block.fn("register_block")

	/** A player's longest reach. */
	const val MAX_RANGE = 16

	/** An admin's (ra.core.Admin): two box selectors of this length every other tick per Fan. */
	const val ADMIN_MAX_RANGE = 64
	const val DEFAULT_RANGE = 8

	/** Strength n pushes at 0.05 × n blocks per tick (1 block a second per step). */
	const val MAX_STRENGTH = 5
	const val ADMIN_MAX_STRENGTH = 10
	const val DEFAULT_STRENGTH = 2

	/** Entities a Fan leaves where they are: pack and map-maker scaffolding, and what hangs on a wall. */
	const val UNMOVED = "fan_unmoved"

	/** The fake player that halves the Fans' pace: they run on the ticks it is 0. */
	private const val PHASE = "#fan.phase"

	fun Ra.registerFan() {
		block(block)
		properties(block) {
			number("range", min = 1, max = MAX_RANGE, adminMax = ADMIN_MAX_RANGE)
			number("strength", min = 1, max = MAX_STRENGTH, adminMax = ADMIN_MAX_STRENGTH)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.FEATHER), count = null) {
			pattern("CFC", "FDF", "CRC")
			key("C", listOf(Items.COBBLESTONE, Items.BLACKSTONE))
			key("F", listOf(Items.FEATHER))
			key("D", listOf(Items.DROPPER))
			key("R", listOf(Items.REDSTONE))
		}

		pack.entityTypeTag(UNMOVED, Ns.INTERACTIVE.id) {
			for (e in listOf(
				EntityTypes.MARKER, EntityTypes.ITEM_DISPLAY, EntityTypes.BLOCK_DISPLAY, EntityTypes.TEXT_DISPLAY,
				EntityTypes.INTERACTION, EntityTypes.AREA_EFFECT_CLOUD, EntityTypes.PAINTING, EntityTypes.ITEM_FRAME,
				EntityTypes.GLOW_ITEM_FRAME, EntityTypes.LEASH_KNOT, EntityTypes.LIGHTNING_BOLT, EntityTypes.END_CRYSTAL,
				EntityTypes.SHULKER, EntityTypes.ENDER_DRAGON,
			)) add(e)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.range", "$DEFAULT_RANGE")
			setOnNew(block, "data.properties.strength", "$DEFAULT_STRENGTH")
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			stackedPropLine("range", "Range: ", "aqua", " blocks")
			stackedPropLine("strength", "Strength: ", "yellow")
		}
		FanPush.register(this)

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)

		pack.define(block.tick) {
			breakCheck(block, Blocks.DROPPER, onBreak)
			// Every other tick: 1, 0, 1, 0...
			raw("scoreboard players add $PHASE ${FanPush.T} 1")
			raw("execute if score $PHASE ${FanPush.T} matches 2.. run scoreboard players set $PHASE ${FanPush.T} 0")
			// Only a powered Fan goes further than its own block state.
			executeRun("if score $PHASE ${FanPush.T} matches 0 as ${block.markerSel()} at @s if block ~ ~ ~ minecraft:dropper[triggered=true]", FanPush.run)
			// Players lifted by an upward Fan they have since left come down.
			executeRun("if entity @a[tag=${FanPush.LIFTED},limit=1]", FanPush.liftEnd)
		}
	}
}
