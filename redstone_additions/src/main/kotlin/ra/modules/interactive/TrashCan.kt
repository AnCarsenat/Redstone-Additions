package ra.modules.interactive

import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.breakCheck
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.forEachMarker
import ra.core.block.gogglesHeader
import ra.core.block.particleViewersSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw

/**
 * Trash Can: a barrel that destroys whatever enters it -- items put in by hand, pushed in by a
 * hopper, an Item Pipe or an Item Mover, and dropped items landing on top of it.
 *
 * A barrel, because everything that moves items already knows how to fill one (#ra_lib:containers,
 * vanilla hoppers), and it fires nothing on a redstone edge.
 *
 * IDLE COST: two tests per can per tick, both answered without walking anything -- "has the barrel
 * a first item" and "is there an item entity in the one block above it" (a box selector, which the
 * game answers from the entities stored in that block's chunk section). The clearing itself runs
 * only when one of them is true. Every tick rather than at a slower pace, because a slower can is
 * a buffer: a hopper under it would have ticks to pull out what it was meant to destroy.
 */
object TrashCan {
	val block = CustomBlock(Ns.INTERACTIVE, "trash_can", "Trash Can", model = "minecraft:barrel")
	val item = block.loreItem { lore(loreLine("Destroys every item put into it", "gray")) }
	val registerBlock = block.fn("register_block")
	private val process = block.fn("process")
	private val clear = block.fn("clear")
	private val eat = block.fn("eat")

	fun Ra.registerTrashCan() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.CACTUS), count = null) {
			pattern("I I", "ICI", "IBI")
			key("I", listOf(Items.IRON_INGOT))
			key("C", listOf(Items.CACTUS))
			key("B", listOf(Items.BARREL))
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(process) { process() }
		pack.define(clear) { clear() }
		pack.define(eat) { eat() }

		// A broken can spills what is in it like any barrel; there is never more than one tick's worth.
		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)

		pack.define(block.tick) {
			breakCheck(block, Blocks.BARREL, onBreak)
			forEachMarker(block, process)
		}
	}

	/** One can. As its marker, at its block. */
	private fun Function.process() {
		executeRun("if data block ~ ~ ~ Items[0]", clear)
		executeRun("positioned ~-0.5 ~0.5 ~-0.5 if entity @e[type=item,dx=0,dy=0,dz=0,limit=1]", eat)
	}

	/** Empty the barrel. As the marker, at the can. */
	private fun Function.clear() {
		raw("data remove block ~ ~ ~ Items")
		effects()
	}

	/**
	 * Destroy the dropped items lying on top. As the marker, at the can.
	 *
	 * Not an item that can never be picked up (`PickupDelay:32767s`): that is another pack's or a
	 * map maker's display item, not something thrown away -- the Magic Crate leaves those too.
	 */
	private fun Function.eat() {
		raw("execute store result score #trash ${Obj.TEMP} positioned ~-0.5 ~0.5 ~-0.5 run kill @e[type=item,dx=0,dy=0,dz=0,nbt=!{PickupDelay:32767s}]")
		raw("execute unless score #trash ${Obj.TEMP} matches 1.. run return 0")
		effects()
	}

	/** A puff of smoke and a hiss, for the players who asked for block particles and sounds. */
	private fun Function.effects() {
		raw("particle minecraft:smoke ~ ~0.6 ~ 0.2 0.1 0.2 0.01 4 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.lava.extinguish block ${soundListenersSel(8)} ~ ~0.5 ~ 0.15 1.8")
	}
}
