package ra.modules.enchanting.schematic

import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.predicates.sub.itemStackPredicate
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.core.Ns
import ra.core.Obj
import ra.core.RA_CHAT_PREFIX
import ra.core.Ra
import ra.core.define
import ra.core.item.SBool
import ra.core.item.SInt
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.flag
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref
import ra.lib.BlockTags
import ra.modules.enchanting.BookPrinter

/**
 * The Schematic: paper (map look) with `custom_data.ra = {schematic:1b, enchantments:{...}}`, drawn
 * from an enchanted book by the [BookPrinter]. The enchantments are kept in custom data and not in
 * an enchantment component, so an anvil, a grindstone or the Disenchanter never takes it for an
 * enchanted book. Its lore lists them, written when it is drawn. It is not craftable.
 *
 * RIGHT-CLICK. Like the Pattern, it is an endless `consumable`, so a right-click fires the
 * `using_item` advancement `ra_enchanting:schematic/use` (every tick the button is held; only the
 * first tick of a press counts, [USED]). Nothing runs while nobody uses one. The look is walked
 * from the eyes as the Pattern's: the first Book Printer it enters takes the Schematic
 * ([BookPrinter.insert], run as its marker with the player tagged [USER]); a solid block or
 * nothing in reach gives a hint.
 */
object Schematic {
	private fun fn(path: String) = ref(Ns.ENCHANTING, "schematic/$path")

	private val onUse = fn("on_use")
	private val step = fn("ray")

	/** `custom_data.ra` flag of a Schematic. */
	const val FLAG = "schematic"

	/** An item test for a Schematic, for `if items ... $HELD`. */
	const val HELD = """*[minecraft:custom_data~{ra:{schematic:1b}}]"""

	/** On the player whose Schematic is being used, for the duration of the use. */
	const val USER = "ra.ench.sch_user"

	/** The game time the player's Schematic was last seen in use. */
	const val USED = "ra.ench.sch_used"

	/** `#sch.hit ra.temp`: set to 1 by [BookPrinter.insert] when the look found a printer. */
	const val HIT = "#sch.hit"

	private const val T = Obj.TEMP
	private const val REACH_STEPS = 50

	/** The Schematic, before the book's enchantments are copied onto it. */
	val item = itemSpec("minecraft:paper") {
		model("minecraft:map")
		name("Schematic")
		rarity("rare")
		glint()
		lore(
			loreLine("Right-click a Book Printer with it,", "dark_gray"),
			loreLine("then throw books on the printer to print copies", "dark_gray"),
		)
		customData(raData(flag(FLAG)))
		add("food", comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)))
		add("consumable", comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)))
	}

	fun Ra.registerSchematic() {
		val useAdvancement = pack.advancement("schematic/use") {
			namespace = Ns.ENCHANTING.id
			criteria {
				usingItem("requirement") {
					this.item = itemStackPredicate(Items.PAPER) {
						predicates { customData { this["ra"] = nbt { this[FLAG] = true } } }
					}
				}
			}
			rewards { function = onUse }
		}

		// As the player, from the advancement, which fires every tick the button is held: only the
		// first tick of a press acts.
		pack.define(onUse) {
			raw("advancement revoke @s only ${useAdvancement.asId()}")
			raw("execute store result score #sch.now $T run time query gametime")
			raw("scoreboard players operation #sch.prev $T = @s $USED")
			raw("scoreboard players operation @s $USED = #sch.now $T")
			raw("scoreboard players remove #sch.now $T 1")
			raw("execute if score #sch.prev $T >= #sch.now $T run return 0")
			raw("execute unless items entity @s weapon.mainhand $HELD run return 0")

			raw("tag @s add $USER")
			raw("scoreboard players set $HIT $T 0")
			raw("scoreboard players set #sch.ray $T $REACH_STEPS")
			// `anchored feet` once at the eyes, as the tools' ray: every `^` step would add the eye height again.
			raw("execute at @s anchored eyes positioned ^ ^ ^ anchored feet run function ${step.asId()}")
			raw("tag @s remove $USER")
			raw("""execute if score $HIT $T matches 0 run tellraw @s [$RA_CHAT_PREFIX,{text:"Right-click a Book Printer with the Schematic to make it print this book.",color:"gray"}]""")
		}

		// One point of the look, as the player.
		pack.define(step) {
			raw("execute align xyz positioned ~0.5 ~0.5 ~0.5 as @e[type=minecraft:marker,tag=${BookPrinter.block.tag},distance=..0.6,limit=1] run return run function ${BookPrinter.insert.asId()}")
			// A block the crosshair stops on ends the walk; not one the player's own body is in.
			raw("execute unless block ~ ~ ~ ${BlockTags.rayPassable.asId()} align xyz unless entity @s[dx=0,dy=0,dz=0] run return 0")
			raw("scoreboard players remove #sch.ray $T 1")
			raw("execute if score #sch.ray $T matches 1.. positioned ^ ^ ^0.1 run function ${step.asId()}")
		}
	}
}
