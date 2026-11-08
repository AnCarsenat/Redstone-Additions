package ra.modules.interactive

import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.Tag
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.breakCheck
import ra.core.block.dropItem
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.blockRegisterMessage
import ra.migrations.DataVersion.stampNew

/**
 * The three Infinite Cauldrons: a cauldron that refills itself every tick, with water, lava or
 * powder snow. Three near-identical blocks, so they are one table here.
 *
 * They place their block themselves (`setblock` + a bare marker) instead of going through
 * ra_lib:placement/place: a filled cauldron has a block state the library does not take.
 */
object Cauldrons {
	/**
	 * @property full the block state of a full cauldron.
	 * @property partial the states to top up besides the empty `cauldron`; lava has no levels.
	 */
	class Cauldron(
		id: String,
		name: String,
		val full: String,
		val partial: List<String>,
		val pattern: List<String>,
		val keys: List<Pair<String, List<ItemArgument>>>,
		val unlock: Unlock,
	) {
		val block = CustomBlock(Ns.INTERACTIVE, id, name, model = "minecraft:cauldron")
		val item = block.loreItem {}
		val registerBlock = block.fn("register_block")
	}

	val water = Cauldron(
		"infinite_water_cauldron", "Infinite Water Cauldron",
		full = "water_cauldron[level=3]", partial = listOf("water_cauldron[level=1]", "water_cauldron[level=2]"),
		pattern = listOf("III", "WCW", "III"),
		keys = listOf("I" to listOf(Items.IRON_INGOT), "W" to listOf(Items.WATER_BUCKET), "C" to listOf(Items.CAULDRON)),
		unlock = Unlock(Items.CAULDRON),
	)
	val lava = Cauldron(
		"infinite_lava_cauldron", "Infinite Lava Cauldron",
		full = "lava_cauldron", partial = emptyList(),
		pattern = listOf("OOO", "LCL", "OOO"),
		keys = listOf("O" to listOf(Items.OBSIDIAN), "L" to listOf(Items.LAVA_BUCKET), "C" to listOf(Items.CAULDRON)),
		unlock = Unlock(Items.LAVA_BUCKET, "has_lava"),
	)
	val snow = Cauldron(
		"infinite_snow_cauldron", "Infinite Snow Cauldron",
		full = "powder_snow_cauldron[level=3]", partial = listOf("powder_snow_cauldron[level=1]", "powder_snow_cauldron[level=2]"),
		pattern = listOf("SSS", "PCP", "SSS"),
		keys = listOf("S" to listOf(Items.SNOW_BLOCK), "P" to listOf(Items.POWDER_SNOW_BUCKET), "C" to listOf(Items.CAULDRON)),
		unlock = Unlock(Items.POWDER_SNOW_BUCKET, "has_powder_snow"),
	)

	/** In load / tick / give order. */
	val all = listOf(water, lava, snow)

	fun Ra.registerCauldrons() = all.forEach { registerCauldron(it) }

	private fun Ra.registerCauldron(c: Cauldron) {
		val block = c.block
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.specGive(block, c.item)
		pack.shapedRecipe(block.ns, block.id, c.item.recipeResult(), c.unlock, RecipeCategory.MISC, count = null) {
			pattern(*c.pattern.toTypedArray())
			for ((k, items) in c.keys) key(k, items)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// The cauldron, full, with a bare marker (no direction).
			raw("setblock ~ ~ ~ ${c.full}")
			raw("""summon marker ~ ~ ~ {Tags:["${Tag.RA}","${Tag.CUSTOM_BLOCK}","${block.tag}"]}""")
			stampNew("@e[type=marker,tag=${block.tag},distance=..0.1,sort=nearest,limit=1]")
			raw(ra.api.Settings.Purge.stampLine("@e[type=marker,tag=${block.tag},distance=..0.1,sort=nearest,limit=1]"))
			raw("playsound minecraft:block.stone.place block ${soundListenersSel()} ~ ~ ~ 1 1")
			returnValue(1)
		}

		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(c.item) }, sound = STONE_BREAK_SOUND, cloud = true)

		pack.define(block.tick) {
			// Break detection only when the block is air: see BreakTest.Air.
			breakCheck(block, BreakTest.Air, onBreak)

			// Refill when emptied or partially filled.
			for (state in listOf("cauldron") + c.partial)
				raw("execute as ${block.markerSel()} at @s if block ~ ~ ~ $state run setblock ~ ~ ~ ${c.full}")
		}
	}
}
