package ra.modules.automation.pattern

import io.github.ayfri.kore.features.itemmodifiers.functions.copyCustomData
import io.github.ayfri.kore.features.itemmodifiers.functions.replace
import io.github.ayfri.kore.features.itemmodifiers.itemModifier
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.TagRoute
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.item.SBool
import ra.core.item.SInt
import ra.core.item.SList
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref
import ra.modules.automation.pattern.PatternUse.registerPatternUse

/**
 * Pattern: an item that stores a 3x3 grid of item ids.
 *
 * DATA FORMAT (a contract: other blocks and add-ons read it, and saved items keep it):
 *
 * ```
 * custom_data: {ra:{pattern:{slots:["minecraft:oak_planks","","",  "","","",  "","",""]}}}
 * ```
 *
 * `slots` always has nine strings, crafter slot order (row by row, top left first, the
 * `container.0`..`container.8` of a crafter). Each is a namespaced item id, or `""` for an empty
 * (or disabled) slot. Only ids: no counts, no components. A blank Pattern has nine `""`. An item is
 * a Pattern when its custom_data contains `ra.pattern` (`*[custom_data~{ra:{pattern:{}}}]`,
 * [HELD]). Readers must treat the ids as player data: an edited item can hold anything, so a
 * value is checked (`ra_lib:safe/check`, format ID) before it is ever pasted into a command.
 *
 * Who reads it: the Crafter Controller (what to wait for), and later the Keypad (a Pattern as a
 * code: two patterns are equal when their `slots` lists are).
 *
 * HOW IT IS SET. Sneak + right-click a vanilla crafter: its nine slots are copied in (see
 * [PatternUse]). Sneak + right-click anything else clears it. Right-click a block that takes
 * patterns (registered in [targets]) to hand it over.
 *
 * STACKING. Patterns stack to [MAX_STACK]. Recording or clearing acts on ONE Pattern: a held stack
 * of two or more loses one and the rewritten Pattern is handed over separately (an item entity at
 * the player, only they can pick it up). A target that keeps a Pattern takes one from the stack.
 */
object Pattern {
	private fun fn(path: String) = ref(Ns.AUTOMATION, "pattern/$path")

	val give = fn("give")

	/** `storage ra_automation:pattern`: the scratch the Pattern functions write through. */
	const val STORE = "ra_automation:pattern"

	/** An item test for a Pattern, for `if items ... $HELD`. */
	const val HELD = """*[minecraft:custom_data~{ra:{pattern:{}}}]"""

	/** Nine empty slots, the blank pattern. */
	val BLANK_SLOTS = List(9) { "" }
	val blankSlotsSnbt = BLANK_SLOTS.joinToString(",", "[", "]") { "\"$it\"" }

	/**
	 * Custom blocks that accept a Pattern: right-clicking one of them with a Pattern runs `fn` as
	 * its marker, at the block's centre, with the player tagged [USER]. The main hand may hold a
	 * stack: a block that keeps the Pattern takes ONE ([TAKE_ONE]), a block that only reads it
	 * leaves the stack alone. The function must set `#pat.hit ra.temp` to 1. Register
	 * before the pack is complete; the ray is written after every part.
	 */
	val targets = mutableListOf<TagRoute>()

	/**
	 * Patterns stack: identical ones (same `slots`, hence the same lore) merge, blank with blank.
	 * The endless `consumable` that detects the use works on a stack as on a single item.
	 */
	const val MAX_STACK = 64

	/** An item modifier taking one item from a stack (an emptied slot becomes air): `item modify ... $TAKE_ONE`. */
	const val TAKE_ONE = """{function:"minecraft:set_count",count:-1,add:true}"""

	/** On the player whose Pattern is being used, for the duration of the use. */
	const val USER = "ra.auto.pattern_user"

	val item = itemSpec("minecraft:paper") {
		model("minecraft:flow_banner_pattern")
		name("Pattern")
		add("max_stack_size", SInt(MAX_STACK))
		customData(raData("pattern" to comp("slots" to SList(BLANK_SLOTS.map { SStr(it) }))))
		lore(*blankLore().toTypedArray())
		add("food", comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)))
		add("consumable", comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)))
	}

	/** The lore of a blank Pattern, as SNBT compounds. */
	fun blankLore() = listOf(
		loreLine("Blank", "dark_gray"),
		loreLine("Sneak + right-click a crafter to record it", "gray"),
	)

	/** The hint lines under a recorded grid. */
	fun hintLore() = listOf(
		loreLine("Right-click a Crafter Controller to load it", "dark_gray"),
		loreLine("Sneak + right-click air to clear", "dark_gray"),
	)

	fun Ra.registerPattern() {
		pack.define(give) { raw("give @s ${item.giveArg()}") }

		pack.shapedRecipe(Ns.AUTOMATION, "pattern", item.recipeResult(Items.PAPER), Unlock(Items.CRAFTER), RecipeCategory.MISC, count = 3) {
			pattern(" R ", "PPP")
			key("R", Items.REDSTONE)
			key("P", Items.PAPER)
		}

		pack.itemModifier("pattern/write") {
			namespace = Ns.AUTOMATION.id
			copyCustomData(STORE) { replace("rec.slots", "ra.pattern.slots") }
		}

		registerPatternUse()
	}
}
