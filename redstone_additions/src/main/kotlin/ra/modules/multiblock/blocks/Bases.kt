package ra.modules.multiblock.blocks

import io.github.ayfri.kore.arguments.components.item.CustomComponent
import io.github.ayfri.kore.arguments.components.item.itemModel
import io.github.ayfri.kore.arguments.maths.Axes
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.selector.Sort
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import net.benwoodworth.knbt.NbtString
import ra.api.MultiblockLib
import ra.api.RaLib
import ra.core.Ns
import ra.core.Ra
import ra.core.Storage
import ra.core.Tag
import ra.core.block.RecipeResult
import ra.core.block.Unlock
import ra.core.block.killVanillaDrop
import ra.core.block.particleViewersSel
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raMessage
import ra.core.raw
import ra.core.ref
import ra.core.registeredLine

/**
 * Multiblock Bases (`ra_multiblock:blocks/...`): the core block every multiblock is assembled
 * around, one block type in five tiers. Give, placement and break are each one macro function
 * fed with `storage ra:multiblock tier_data.<tier>` (written by ra_multiblock:load); the per-tier
 * functions are thin wrappers so the placement router can list them.
 */
object Bases {
	val baseGive = ref(Ns.MULTIBLOCK, "blocks/base_give")
	val baseHandlePlacement = ref(Ns.MULTIBLOCK, "blocks/base_handle_placement")
	val baseOnBreak = ref(Ns.MULTIBLOCK, "blocks/base_on_break")
	val checkBreak = ref(Ns.MULTIBLOCK, "blocks/check_break")
	val giveAll = ref(Ns.MULTIBLOCK, "blocks/give_all")
	val registerAll = ref(Ns.MULTIBLOCK, "blocks/register_all")

	/** On every base marker, so the module tick can find them all in one selector. */
	const val BASE_TAG = "ra.multiblock_base"

	fun handlePlacement(t: BaseTier) = ref(Ns.MULTIBLOCK, "blocks/${t.dir}/handle_placement")

	fun Ra.registerBases() {
		pack.define(baseGive) { baseGive() }
		pack.define(baseHandlePlacement) { baseHandlePlacement() }
		pack.define(baseOnBreak) { baseOnBreak() }

		pack.define(checkBreak) {
			for (t in BaseTier.all) execute {
				ifCondition { entity(self { tag = t.markerTag }) }
				unlessCondition { block(vec3(), t.block) }
				run { returnRun { function(baseOnBreak, Storage.MULTIBLOCK, t.path) } }
			}
		}

		pack.define(giveAll) {
			for (t in BaseTier.all) function(baseGive, Storage.MULTIBLOCK, t.path)
			raMessage("@s", "Given all multiblock bases!", "green")
		}

		// Announces the bases on load, for anyone wearing ra.debug. One block type with five
		// tiers, so five lines: knowing the module loaded is not the same as knowing which tiers
		// it knows about.
		pack.define(registerAll) {
			for (t in BaseTier.all) registeredLine(t.name)
		}

		for (t in BaseTier.all) {
			// Thin wrapper, delegates to the macro.
			placementHandlers += pack.define(handlePlacement(t)) { function(baseHandlePlacement, Storage.MULTIBLOCK, t.path) }
			recipe(t)
		}

		// The tier is what the wrench assembles against; nothing may edit it. Keyed by each tier's
		// own type, multiblock_base.<tier>: the old `readonly("multiblock_base", ...)` named a type
		// no base carries, so the tier was editable after all.
		for (t in BaseTier.all) properties(t.blockTag) {
			text("tier", readonly = true)
			number("tier_level", min = 1, max = 5, readonly = true)
		}
	}

	/** `$(tier)` macro arguments: see [BaseTier.tierDataSnbt]. */
	private fun Function.baseGive() {
		raw("""${'$'}give @s bat_spawn_egg[item_model="$(block_model)",item_name="$(tier_name) Multiblock Base",lore=[{text:"$(lore_text)",color:"gray",italic:false}],custom_data={ra:{multiblock_base:"$(tier)"}},entity_data={id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.multiblock_base","ra.place.multiblock_base.$(tier)"],Silent:1b,NoAI:1b,Invulnerable:1b}]""")
	}

	/** As the spawned bat, at its position. Macro: tier data. */
	private fun Function.baseHandlePlacement() {
		raw("\$execute unless entity @s[tag=ra.place.multiblock_base.$(tier)] run return 0")

		// Place the block (dir_type 0 = no facing).
		raw("\$function ${RaLib.Placement.place.asId()} {block_id:\"$(block_id)\",block_tag:\"$(block_tag)\",dir_type:0}")

		raw("\$data modify entity @e[type=marker,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.tier set value \"$(tier)\"")
		raw("\$data modify entity @e[type=marker,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.tier_level set value $(tier_level)")

		// The base tag, for break detection.
		raw("tag @e[type=marker,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add $BASE_TAG")

		raw("\$tag @e[type=marker,tag=ra.custom_block.$(block_tag),tag=ra.new,distance=..0.5] remove ra.new")

		raw("\$playsound $(place_sound) block ${soundListenersSel(16)} ~ ~ ~ $(place_volume) $(place_pitch)")
		raw("particle minecraft:electric_spark ~ ~0.5 ~ 0.3 0.3 0.3 0.05 15 normal ${particleViewersSel()}")

		returnValue(1)
	}

	/** As the base marker, at its position. Macro: tier data. */
	private fun Function.baseOnBreak() {
		killVanillaDrop("$(block_id)")

		// Disassemble any linked multiblock (aligned position + tight distance).
		execute {
			align(Axes.XYZ)
			positioned(vec3(0.5, 0.0, 0.5).relative)
			asTarget(allEntities(limitToOne = true) {
				type = EntityTypes.MARKER
				tag = Tag.MULTIBLOCK
				distance = rangeOrDoubleEnd(0.5)
				sort = Sort.NEAREST
			})
			run { function(MultiblockLib.disassemble) }
		}

		raw("""${'$'}summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"$(block_model)","minecraft:item_name":"$(tier_name) Multiblock Base","minecraft:custom_data":{ra:{multiblock_base:"$(tier)"}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.multiblock_base","ra.place.multiblock_base.$(tier)"],Silent:1b,NoAI:1b,Invulnerable:1b},"minecraft:lore":[{text:"$(lore_text)",color:"gray",italic:false}]}}}""")

		raw("\$playsound $(break_sound) block ${soundListenersSel(16)} ~ ~ ~ $(break_volume) $(break_pitch)")
		raw("particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5 normal ${particleViewersSel()}")

		kill(self())
	}

	private fun Ra.recipe(t: BaseTier) {
		// CustomComponent (raw NBT): Kore's typed item_name / lore / custom_data / entity_data
		// only serialize to SNBT and throw inside a recipe's JSON.
		val result = RecipeResult(Items.BAT_SPAWN_EGG) {
			itemModel(t.blockId.substringAfter(':'), "minecraft")
			this["item_name"] = CustomComponent(NbtString(t.name))
			this["lore"] = CustomComponent(t.loreNbt())
			this["custom_data"] = CustomComponent(t.customDataNbt())
			this["entity_data"] = CustomComponent(t.entityDataNbt())
		}
		// count = null: the beet recipes left the count out.
		pack.shapedRecipe(Ns.MULTIBLOCK, "${t.tier}_base", result, Unlock(t.unlockedBy), RecipeCategory.MISC, count = null) {
			pattern(t.ringKey.repeat(3), t.ringKey + t.coreKey + t.ringKey, t.ringKey.repeat(3))
			key(t.ringKey, t.ring)
			key(t.coreKey, t.core)
		}
	}
}
