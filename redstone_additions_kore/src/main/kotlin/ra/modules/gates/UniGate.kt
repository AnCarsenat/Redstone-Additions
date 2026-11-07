package ra.modules.gates

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.summon
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.recipes
import io.github.ayfri.kore.features.advancements.requirements
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.recipeUnlocked
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShaped
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.killVanillaDrop
import ra.core.block.markers
import ra.core.block.newMarker
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.allEntities
import ra.core.define
import ra.core.raw

/**
 * UNI Gate: one universal logic gate whose `gate_type` property picks the function (and, or,
 * not, xor, nand, nor, xnor). Replaced the seven separate gate blocks. The wrench cycles the
 * type (ra:tools/wrench/cycle_uni_gate, owned by Tools).
 */
object UniGate {
	val block = CustomBlock(Ns.GATES, "uni_gate", "UNI Gate", model = "minecraft:smooth_stone_slab")
	val process = block.fn("process")
	val onBreakDrop = block.fn("on_break_drop")

	private const val DISPLAY_TAG = "ra.custom_block.display_item.uni_gate"
	private const val OUT = "ra.out_success"
	private const val WAS_ON = "ra.gate.was_on"
	private const val SYNCED = "ra.gate.was_on_synced"

	fun Ra.registerUniGate() {
		block(block)
		pack.blockGive(block)
		recipe()
		pack.registerBlockMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(onBreakDrop) { onBreakDrop() }
		pack.define(process) { process() }
		pack.gateGoggles(block, nameY = 0.7, prop = "gate_type", label = "Mode: ", suffix = "", y = 0.45)
		pack.define(block.tick) { tick() }
	}

	/**
	 * The recipe, and its unlock advancement written by hand rather than through `blockRecipe`:
	 * its criterion has always been called `has_slab`, and a renamed criterion drops the progress
	 * saved under the old name.
	 */
	private fun Ra.recipe() {
		val recipe = pack.recipesBuilder.craftingShaped(block.id, block.ns.id) {
			category = RecipeCategory.REDSTONE
			showNotification = true
			pattern(" S ", "TRT", " S ")
			key("S", Items.SMOOTH_STONE_SLAB)
			key("R", Items.REPEATER)
			key("T", Items.REDSTONE_TORCH)
			result(Items.BAT_SPAWN_EGG, 1) { with(block) { blockItemComponents() } }
		}
		pack.advancement("recipes/unlock_${block.id}") {
			namespace = block.ns.id
			parent = AdvancementArgument("recipes/root", "minecraft")
			criteria {
				recipeUnlocked("has_the_recipe", recipe)
				inventoryChanged("has_slab") { item { items = listOf(Items.SMOOTH_STONE_SLAB) } }
			}
			requirements(listOf("has_the_recipe", "has_slab"))
			rewards { recipes(recipe) }
		}
	}

	/**
	 * As the spawned bat, at the block centre. Unlike the other gates it leaves the bat's place
	 * tag alone, as the beet version did.
	 */
	private fun Function.handlePlacement() {
		placementGuard(block)
		placeBlock(block)

		// Display offsets are measured from the marker, which sits at the BLOCK CENTRE (y + 0.5),
		// not at the block's floor. An offset of ~0.5 therefore lands a full block above the floor
		// -- half a block clear of a slab's top face, which is why these displays floated. The
		// offset is (surface height - 0.5) plus a hair to keep the item out of the surface.
		// Slab top face is at 0.5, so the offset from the centre is 0.
		killDisplays(0.6)
		raw("""summon item_display ~ ~0.01 ~ {item:{id:"minecraft:comparator",count:1},item_display:"fixed",transformation:{left_rotation:[0.7071f,0f,0f,0.7071f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]},Tags:["ra","ra.custom_block.display_item","$DISPLAY_TAG"]}""")

		// Default gate_type: and (also or, not, xor, nand, nor, xnor).
		data(newMarker(block)) { modify("data.properties.gate_type") { set("and") } }
		val fresh = allEntities {
			type = EntityTypes.MARKER
			tag = block.tag
			tag = Tag.NEW
			distance = rangeOrDoubleEnd(0.5)
		}
		scoreboard.players.set(fresh, Obj.COOLDOWN, 0)

		tag(fresh) { remove(Tag.NEW) }
		returnValue(1)
	}

	/** Kills the gate's item displays within [radius]. */
	private fun Function.killDisplays(radius: Double) {
		kill(allEntities { type = EntityTypes.ITEM_DISPLAY; tag = block.tag; distance = rangeOrDoubleEnd(radius) })
		kill(allEntities { type = EntityTypes.ITEM_DISPLAY; tag = DISPLAY_TAG; distance = rangeOrDoubleEnd(radius) })
	}

	/** Cleans up a broken gate and returns its item. As the marker, at its position. */
	private fun Function.onBreakDrop() {
		killVanillaDrop(block.physical)
		killDisplays(2.0)
		shell(on = false)
		summon(EntityTypes.ITEM, vec3(), nbt {
			this["Item"] = block.itemStackNbt()
			this["Tags"] = NbtList(listOf(NbtString(Tag.RA), NbtString(Tag.DROP_TEMP)))
		})
		kill(self())
	}

	private fun Function.tick() {
		// Already one function per broken marker in the beet version, so kept as it was.
		raw("execute as @e[type=marker,tag=${block.tag}] at @s unless block ~ ~ ~ smooth_stone_slab run tag @s add ${Tag.BROKEN}")
		execute {
			asTarget(brokenMarkers())
			at(self())
			run { function(onBreakDrop) }
		}
		tag(brokenMarkers()) { remove(Tag.BROKEN) }

		// Force the physical output back in sync every 40 ticks. process only rewrites the shell
		// when its own result changes, so this is what repairs a gate whose blocks were altered by
		// hand, and what brings pre-existing gates in line the first time this version runs.
		val resync = literal("#uni_gate_resync")
		scoreboard.players.add(resync, Obj.TEMP, 1)
		execute {
			ifCondition { score(resync, Obj.TEMP, rangeOrIntStart(40)) }
			run { tag(markers(block)) { remove(SYNCED) } }
		}
		execute {
			ifCondition { score(resync, Obj.TEMP, rangeOrIntStart(40)) }
			run { scoreboard.players.set(resync, Obj.TEMP, 0) }
		}

		execute {
			asTarget(markers(block))
			at(self())
			run { function(process) }
		}
	}

	private fun brokenMarkers() = ra.core.block.brokenMarkers(block)

	/** As the marker, at its position. */
	private fun Function.process() {
		fun gate(type: String) = """data.properties{gate_type:"$type"}"""

		execute {
			unlessCondition { data(self(), "data.properties.gate_type") }
			run { data(self()) { modify("data.properties.gate_type") { set("and") } } }
		}

		// Detect redstone for this gate. ignore_blocks stays on for the input census too,
		// otherwise the gate's own redstone_block output shell would read back as six extra inputs.
		tag(self()) { add(IGNORE_BLOCKS) }
		function(ra.api.RaLib.Redstone.detect)

		// AND and NAND are the only modes that must tell "input present but off" apart from "no
		// input on that side", so the census only runs for them.
		scoreboard.players.set(self(), Obj.RS_INPUTS, 0)
		for (type in listOf("and", "nand")) execute {
			ifCondition { data(self(), gate(type)) }
			run { function(ra.api.RaLib.Redstone.countInputs) }
		}

		tag(self()) { remove(IGNORE_BLOCKS) }

		tag(self()) { remove(OUT) }

		// Count powered input directions.
		scoreboard.players.set(self(), Obj.TEMP, 0)
		for (side in listOf(Obj.POWER_NORTH, Obj.POWER_SOUTH, Obj.POWER_EAST, Obj.POWER_WEST, Obj.POWER_UP, Obj.POWER_DOWN)) execute {
			ifCondition { score(self(), side, rangeOrIntStart(1)) }
			run { scoreboard.players.add(self(), Obj.TEMP, 1) }
		}

		val t = Obj.TEMP
		// AND: at least one input, and every input that exists is powered. Comparing the powered
		// count against the census covers unpowered repeaters, comparators, torches and buttons,
		// not just dust and levers.
		raw("execute if data entity @s ${gate("and")} if score @s $t matches 1.. if score @s $t = @s ${Obj.RS_INPUTS} run tag @s add $OUT")
		// OR: any input powered.
		raw("execute if data entity @s ${gate("or")} if score @s $t matches 1.. run tag @s add $OUT")
		// NOT: no input powered.
		raw("execute if data entity @s ${gate("not")} if score @s $t matches 0 run tag @s add $OUT")
		// XOR: exactly one input powered.
		raw("execute if data entity @s ${gate("xor")} if score @s $t matches 1 run tag @s add $OUT")
		// NAND: inverse of AND -- true when nothing is powered, or when some existing input is not.
		raw("execute if data entity @s ${gate("nand")} if score @s $t matches 0 run tag @s add $OUT")
		raw("execute if data entity @s ${gate("nand")} unless score @s $t = @s ${Obj.RS_INPUTS} run tag @s add $OUT")
		// NOR: no input powered (inverse of OR).
		raw("execute if data entity @s ${gate("nor")} if score @s $t matches 0 run tag @s add $OUT")
		// XNOR: not exactly one input powered (inverse of XOR).
		raw("execute if data entity @s ${gate("xnor")} unless score @s $t matches 1 run tag @s add $OUT")

		// Set the output. The fill rewrites a 3x3x3 volume and triggers block updates, so running
		// it every tick cost 27 block writes per gate per tick even while the output stood still.
		// Write only when the result actually changes.
		//
		// ra.gate.was_on_synced records that ra.gate.was_on is known to match the blocks in the
		// world. It is absent on a gate that never ran under this version and is cleared
		// periodically by uni_gate/tick; while absent the output is written unconditionally --
		// that is what repairs a hand-edited shell and migrates gates placed before the
		// change-detection existed.
		raw("execute if entity @s[tag=!$SYNCED,tag=$OUT] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute if entity @s[tag=!$SYNCED,tag=!$OUT] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		raw("execute if entity @s[tag=$SYNCED,tag=$OUT,tag=!$WAS_ON] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute if entity @s[tag=$SYNCED,tag=!$OUT,tag=$WAS_ON] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")

		tag(self()) { add(SYNCED) }
		raw("tag @s[tag=$OUT] add $WAS_ON")
		raw("tag @s[tag=!$OUT] remove $WAS_ON")
	}
}
