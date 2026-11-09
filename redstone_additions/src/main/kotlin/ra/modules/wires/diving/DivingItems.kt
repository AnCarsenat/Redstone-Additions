package ra.modules.wires.diving

import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.item.ItemSpec
import ra.core.raw
import ra.core.rawJson
import ra.core.ref

/**
 * The diving gear: the Air Tank (a chestplate that breathes for you under water, filled at an
 * Air Charger from a gas network's oxygen) and the Heavy Boots (you sink and walk on the bottom).
 *
 * THE TANK BREATHES THROUGH AN ATTRIBUTE, NOT AN EFFECT. A charged tank carries a hidden
 * `oxygen_bonus` modifier at the attribute's maximum (1024): the game then spends one bubble of
 * the player's own air about every 1025 ticks instead of every tick, so fifteen minutes under
 * water cost some 18 bubbles of 300. No status effect is given, so there is no icon blinking at
 * the top of the screen, nothing to take back from the player when the tank comes off, and
 * nothing to clean up on uninstall: the modifier goes with the item. [AirTank] only counts the
 * seconds down and swaps the modifier out when the tank is empty.
 *
 * THE BOOTS COST NOTHING AT ALL. Walking speed in water is a plain item modifier
 * (`water_movement_efficiency`, Depth Strider III's value), which only ever acts in water. The
 * weight is the boots' own enchantment, `ra_wires:heavy`: a `location_changed` effect, like Soul
 * Speed's, adds gravity while the wearer is in water and the game takes it away again once they
 * step out, so a jump on land is a normal jump. No function runs for either item.
 */
object DivingItems : PackPart {
	/** A full tank, in seconds of breathing under water: fifteen minutes. */
	const val AIR_MAX = 900

	/** Oxygen spent per second of breathing (mL). A full tank is [AIR_MAX] x this = 9000 mL. */
	const val ML_PER_SECOND = 10

	/** The tank's own `oxygen_bonus` modifier id. */
	const val BREATH_MODIFIER = "ra_wires:air_tank"

	/** The modifiers of an empty tank: an iron chestplate's armour, and no breath. */
	const val MODS_EMPTY = """[{type:"minecraft:armor",amount:6.0d,operation:"add_value",id:"minecraft:armor.chestplate",slot:"chest"}]"""

	/** A charged tank: the same armour, and the hidden breath. */
	const val MODS_CHARGED = """[{type:"minecraft:armor",amount:6.0d,operation:"add_value",id:"minecraft:armor.chestplate",slot:"chest"},{type:"minecraft:oxygen_bonus",amount:1024.0d,operation:"add_value",id:"$BREATH_MODIFIER",slot:"chest",display:{type:"hidden"}}]"""

	/** The first lore line, kept when [AirTank] rewrites the air line under it. */
	const val TANK_LORE_TOP = """{text:"Breathes for you under water",color:"gray",italic:false}"""
	const val TANK_LORE_HINT = """{text:"Fill it standing on an Air Charger",color:"dark_gray",italic:false}"""

	/** Iron boots' armour, and walking pace on the bottom. */
	private const val BOOTS_MODS = """[{type:"minecraft:armor",amount:2.0d,operation:"add_value",id:"minecraft:armor.boots",slot:"feet"},{type:"minecraft:water_movement_efficiency",amount:1.0d,operation:"add_value",id:"ra_wires:heavy_boots",slot:"feet"}]"""

	/** Gravity x4 in water: you sink at about 2 blocks a second, and still swim up when you jump. */
	private const val HEAVY_GRAVITY = 3.0

	val airTank = ItemSpec.fromSnbt(
		"minecraft:iron_chestplate",
		"item_name" to "\"Air Tank\"",
		"rarity" to "\"uncommon\"",
		"lore" to """[$TANK_LORE_TOP,{text:"Air: ",color:"gray",italic:false,extra:[{text:"empty",color:"red"}]},$TANK_LORE_HINT]""",
		"custom_data" to "{ra:{air_tank:1b,air:0}}",
		"attribute_modifiers" to MODS_EMPTY,
	)

	val heavyBoots = ItemSpec.fromSnbt(
		"minecraft:iron_boots",
		"item_name" to "\"Heavy Boots\"",
		"rarity" to "\"uncommon\"",
		"lore" to """[{text:"You sink in water and walk on the bottom",color:"gray",italic:false},{text:"at your usual pace",color:"gray",italic:false}]""",
		"custom_data" to "{ra:{heavy_boots:1b}}",
		"attribute_modifiers" to BOOTS_MODS,
		"enchantments" to "{\"ra_wires:heavy\":1}",
	)

	val giveAirTank = ref(Ns.WIRES, "items/give_air_tank")
	val giveHeavyBoots = ref(Ns.WIRES, "items/give_heavy_boots")

	override fun Ra.register() {
		pack.define(giveAirTank) { give(airTank, "Air Tank") }
		pack.define(giveHeavyBoots) { give(heavyBoots, "Heavy Boots") }

		// Not in any enchanting table, loot or trade tag: only the boots carry it. Raw JSON: see
		// rawJson for what Kore cannot write here. The requirement is re-tested by the game each
		// time the wearer's block position changes, which is when they step into or out of water.
		pack.rawJson(Ns.WIRES, "enchantment", "heavy", """
			{
			  "description": "Heavy",
			  "supported_items": "minecraft:iron_boots",
			  "weight": 1,
			  "max_level": 1,
			  "min_cost": {"base": 1, "per_level_above_first": 0},
			  "max_cost": {"base": 1, "per_level_above_first": 0},
			  "anvil_cost": 8,
			  "slots": ["feet"],
			  "effects": {
			    "minecraft:location_changed": [
			      {
			        "effect": {
			          "type": "minecraft:attribute",
			          "id": "ra_wires:heavy_boots_gravity",
			          "attribute": "minecraft:gravity",
			          "operation": "add_multiplied_base",
			          "amount": $HEAVY_GRAVITY
			        },
			        "requirements": {
			          "type": "minecraft:entity_properties",
			          "entity": "this",
			          "predicate": {"minecraft:flags": {"is_in_water": true}}
			        }
			      }
			    ]
			  }
			}
		""")

		pack.shapedRecipe(Ns.WIRES, "air_tank", airTank.recipeResult(Items.IRON_CHESTPLATE), Unlock(Items.IRON_CHESTPLATE), RecipeCategory.EQUIPMENT) {
			pattern("ICI", "BXB", " I ")
			key("I", Items.IRON_INGOT); key("C", Items.COPPER_INGOT); key("B", Items.GLASS_BOTTLE); key("X", Items.IRON_CHESTPLATE)
		}
		pack.shapedRecipe(Ns.WIRES, "heavy_boots", heavyBoots.recipeResult(Items.IRON_BOOTS), Unlock(Items.IRON_BOOTS), RecipeCategory.EQUIPMENT) {
			pattern("IXI", "P P")
			key("I", Items.IRON_INGOT); key("X", Items.IRON_BOOTS); key("P", Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
		}
	}

	private fun Function.give(item: ItemSpec, name: String) {
		raw("give @s ${item.giveArg()}")
		raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"$name",color:"aqua"}]""")
	}
}
