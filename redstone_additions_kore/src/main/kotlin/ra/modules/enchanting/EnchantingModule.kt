package ra.modules.enchanting

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.tags.functionTag
import io.github.ayfri.kore.arguments.types.resources.tagged.FunctionTagArgument
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Settings
import ra.core.GameplayModule
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.enchanting.sacrifice.Sacrifice
import ra.modules.enchanting.sacrifice.Sacrifice.registerSacrifice
import ra.modules.infinite.loadMessage

/**
 * Enchant Crafting (`ra_enchanting`): sacrifice crafting on a vanilla enchanting table.
 *
 * Drop items on a table. Once a second the table eats one item off the stack and rolls: on a hit
 * it spits out the upgraded item, on a miss the item is simply gone. The table itself is untouched
 * vanilla -- the mechanic is entirely in the item scan.
 *
 * Recipes come from the function tag `#ra_enchanting:recipes`; each addon adds one function there
 * and never needs to touch this namespace. A recipe function reads `storage ra:enchant input` (the
 * sacrificed item compound) and writes `result` (the item to produce, absent = not mine) and
 * `chance` (percent, 1..100, defaults to 5). The first recipe that matches wins: write recipes
 * with [sacrificeRecipe], which steps aside when an earlier recipe already wrote `result`.
 */
object EnchantingModule : GameplayModule {
	override val ns = Ns.ENCHANTING
	override val title = "Enchant Crafting"

	/** Ticks (in scan steps, not game ticks) since a sacrificed stack last rolled. */
	const val CD = "ra.ench.cd"

	/** Counts game ticks between item scans, so the scan is not paid every tick. */
	const val SCAN = "ra.ench.scan"

	const val ON_TABLE = "ra.ench.on_table"

	/** On items a sacrifice produced: skipped by every later scan. */
	const val DONE = "ra.ench.done"

	/** `#ra_enchanting:recipes`. */
	val recipesTag = FunctionTagArgument("recipes", Ns.ENCHANTING.id)
	val match = ref(Ns.ENCHANTING, "recipes/match")

	override fun Ra.register() {
		// Owned here; ra_infinite, ra_jetpacks and ra_interactive add their own enchant_recipes,
		// see hookEnchantRecipes.
		pack.functionTag(recipesTag.name, recipesTag.namespace)

		registerSacrifice()

		// Ask every registered recipe list whether it wants this item.
		// Input:  storage ra:enchant input   -- the sacrificed item compound
		// Output: storage ra:enchant result  -- the item to produce (absent = no match)
		//         storage ra:enchant chance  -- success chance in percent
		pack.define(match) {
			function(recipesTag)
			// A recipe that names no chance is a 5% recipe.
			raw("execute if data storage ra:enchant result unless data storage ra:enchant chance run data modify storage ra:enchant chance set value 5")
		}

		pack.define(load) {
			scoreboard.objectives.add(CD)
			scoreboard.objectives.add(SCAN)
			scoreboard.players.set(literal("#scan"), SCAN, 0)

			data(Storage.ENCHANT) { merge(nbt { this["input"] = nbt {}; this["chance"] = 0 }) }
			data(Storage.ENCHANT) { remove("result") }

			loadMessage("Enchant crafting loaded!", "light_purple")
		}

		pack.define(tick!!) { tick() }
	}

	/**
	 * Look for sacrificed items lying on an enchanting table.
	 *
	 * This is the one global item selector in the pack, so it runs once every five ticks instead
	 * of every tick. A sacrificed stack rolls once per second, which is four scan steps -- see
	 * sacrifice/on_table.
	 */
	private fun io.github.ayfri.kore.functions.Function.tick() {
		// Defaults to ON. A settings lookup that fails must not be able to switch a module off --
		// see ra_settings:get for why this used to be possible.
		execute {
			storeResult { score(literal("#ec.on"), Obj.TEMP) }
			run { function(Settings.get, nbt { this["key"] = "enchanting"; this["default"] = 1 }) }
		}
		raw("execute if score #ec.on ${Obj.TEMP} matches ..0 run return 0")

		scoreboard.players.add(literal("#scan"), SCAN, 1)
		raw("execute unless score #scan $SCAN matches 5.. run return 0")
		scoreboard.players.set(literal("#scan"), SCAN, 0)

		// Items already produced by a sacrifice carry ra.ench.done and are skipped, so a result
		// never immediately feeds itself back into the table it landed on.
		tag(allEntities { type = EntityTypes.ITEM; tag = ON_TABLE }) { remove(ON_TABLE) }

		// An item resting on a table sits inside the table's own block space; one that is still
		// bouncing sits in the space above it.
		raw("execute as @e[type=item,tag=!$DONE] at @s if block ~ ~ ~ minecraft:enchanting_table run tag @s add $ON_TABLE")
		raw("execute as @e[type=item,tag=!$DONE,tag=!$ON_TABLE] at @s if block ~ ~-1 ~ minecraft:enchanting_table run tag @s add $ON_TABLE")

		execute {
			asTarget(allEntities { type = EntityTypes.ITEM; tag = ON_TABLE })
			at(self())
			run { function(Sacrifice.onTable) }
		}
	}
}
