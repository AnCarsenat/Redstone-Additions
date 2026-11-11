package ra.modules.farming

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import ra.core.block.markers
import io.github.ayfri.kore.features.tags.entityTypeTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.modules.farming.BreederBreeds.registerBreederBreeds
import ra.modules.interactive.specGive
import ra.modules.farming.BreederBreeds.tryBreed
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.callWith
import ra.core.macroCall
import ra.core.block.placementGuard
import ra.core.block.dropItem
import ra.core.block.blockOnBreak
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.markerSel
import ra.core.block.soundListenersSel
import ra.core.block.particleViewersSel
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/**
 * Breeder: feeds the animals in front of it from its own inventory while powered.
 *
 * A barrel wearing a dispenser skin. A dispenser fires its own inventory on any rising redstone
 * edge, so a breeder loaded with wheat spat the wheat across the field the moment you powered
 * it. A barrel is the same inventory with none of that, and ra_lib:skin/apply puts the dispenser
 * face back on the outside.
 */
object Breeder {
	val block = CustomBlock(Ns.INTERACTIVE, "breeder", "Breeder", model = "minecraft:dispenser", physical = "minecraft:barrel", dirType = 2)
	val item = block.loreItem {}
	val registerBlock = block.fn("register_block")

	val breedSuccess = block.fn("breed_success")
	val breedSuccessBucket = block.fn("breed_success_bucket")
	private val consumeItem = block.fn("consume_item")
	private val refreshDisplay = block.fn("refresh_display")
	private val tryBreed = block.fn("try_breed")

	/** Every animal any breed function looks for, for try_breed's one-command gate. */
	const val BREEDABLE_ID = "ra_interactive:breedable"

	fun Ra.registerBreeder() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.HAY_BLOCK), count = null) {
			pattern("CCC", "CHC", "CRC")
			key("C", listOf(Items.COBBLESTONE, Items.BLACKSTONE, Items.COBBLED_DEEPSLATE))
			key("H", listOf(Items.HAY_BLOCK))
			key("R", listOf(Items.REDSTONE))
		}

		pack.entityTypeTag("breedable", Ns.INTERACTIVE.id) {
			for (e in listOf(
				EntityTypes.ARMADILLO, EntityTypes.AXOLOTL, EntityTypes.BEE, EntityTypes.CAMEL,
				EntityTypes.CAT, EntityTypes.CHICKEN, EntityTypes.COW, EntityTypes.DONKEY,
				EntityTypes.FOX, EntityTypes.FROG, EntityTypes.GOAT, EntityTypes.HOGLIN,
				EntityTypes.HORSE, EntityTypes.LLAMA, EntityTypes.MOOSHROOM, EntityTypes.PANDA,
				EntityTypes.PIG, EntityTypes.RABBIT, EntityTypes.SHEEP, EntityTypes.SNIFFER,
				EntityTypes.STRIDER, EntityTypes.TRADER_LLAMA, EntityTypes.TURTLE, EntityTypes.WOLF,
			)) add(e)
		}

		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(refreshDisplay) { refreshDisplay() }
		pack.define(tryBreed) { tryBreed() }
		pack.define(breedSuccess) { breedSuccess(bucket = false) }
		pack.define(breedSuccessBucket) { breedSuccess(bucket = true) }
		pack.define(consumeItem) {
			// Macro `{breed_item}`: the item id without its `minecraft:` prefix.
			raw("\$${macroCall(RaLib.Inventory.remove)} {id:\"minecraft:\$(breed_item)\",count:1}")
		}
		registerBreederBreeds()

		// The vanilla drop goes, and the custom item is summoned in its place. This ran bare, so it
		// killed whatever barrel happened to be within two blocks of wherever the tick function
		// stood -- never the one that had just dropped out of this block. Breaking one handed back
		// both items.
		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true) {
			function(RaLib.Skin.clear, nbt { this["id"] = block.id })
		}

		pack.define(block.tick) {
			breakCheck(block, Blocks.BARREL, onBreak)

			// Repaint the skin if it has gone missing.
			executeRun("as ${block.markerSel()} at @s align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.breeder,distance=..0.4,limit=1]", refreshDisplay)

			// A barrel has no `triggered` state to read, so the redstone is read directly.
			// ra_lib:redstone/any is the cheap reader: it stops at the first live side.
			execute {
				asTarget(markers(block))
				at(self())
				ifCondition { function(RaLib.Redstone.any) }
				ifCondition { data(vec3(), "Items[0]") }
				run { function(tryBreed) }
			}
		}
	}

	/** As the spawned bat, at its position. */
	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		executeRun("at @e[type=marker,tag=${block.tag},tag=${Tag.NEW},limit=1,sort=nearest]", refreshDisplay)
		finishPlacement(block, removePlaceTag = false, withinBlock = false)
	}

	/**
	 * Rebuild the dispenser skin. At the breeder's block position.
	 *
	 * The block underneath is a barrel, because the breeder holds the food it feeds and a vanilla
	 * dispenser throws its own contents on any rising redstone edge. See ra_lib:skin/apply.
	 */
	private fun Function.refreshDisplay() {
		function(RaLib.Skin.apply, nbt {
			this["real"] = block.physical
			this["skin"] = block.model
			this["id"] = block.id
		})
	}

	/**
	 * A pair bred. Positioned at the breeder. The plain one consumes one `breed_item` from the
	 * breeder's own inventory; the bucket one turns the tropical fish bucket back into a water
	 * bucket. The hearts and the sound are in front, where the animals are.
	 */
	private fun Function.breedSuccess(bucket: Boolean) {
		raw("playsound minecraft:entity.experience_orb.pickup block ${soundListenersSel(8)} ^ ^ ^1 0.5 1.2")
		raw("particle minecraft:heart ^ ^ ^1 0.5 0.5 0.5 0.02 5 normal ${particleViewersSel()}")
		if (bucket) raw("""data modify block ~ ~ ~ Items[{id:"minecraft:tropical_fish_bucket"}].id set value "minecraft:water_bucket"""")
		else callWith(consumeItem, "storage ra:temp")
		// Mark success: the rest of try_breed is skipped.
		scoreboard.players.set(self(), Obj.TEMP, 1)
	}

}
