package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage
import ra.core.executeRun
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

/** Block Placer: a dispenser that places its first stack as a block in front, once a second while powered. */
object BlockPlacer {
	val block = CustomBlock(Ns.INTERACTIVE, "block_placer", "Block Placer", model = "minecraft:dispenser", dirType = 2)
	val item = block.loreItem {}
	val registerBlock = block.fn("register_block")
	private val fire = block.fn("fire")
	private val placeBlockFn = block.fn("place_block")

	fun Ra.registerBlockPlacer() {
		block(block)
		properties(block) {
			number("cooldown", min = 1, readonly = true)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.DISPENSER), count = null) {
			pattern("CCC", "BDR", "CCC")
			key("C", listOf(Items.COBBLESTONE, Items.BLACKSTONE))
			key("B", listOf(Items.BOW))
			key("D", listOf(Items.DISPENSER))
			key("R", listOf(Items.REDSTONE_BLOCK))
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(fire) { fire() }
		pack.define(placeBlockFn) { placeFromMacro() }

		val onBreak = pack.blockOnBreak(
			block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true,
			afterVanillaKill = { dropFirstSlot() },
		)

		pack.define(block.tick) {
			breakCheck(block, Blocks.DISPENSER, onBreak)

			// One placement per second while held. This used to fire on every tick a signal was
			// present, which emptied a stocked placer in under two seconds and made it impossible
			// to use with anything but a single pulse.
			raw("scoreboard players add ${block.markerSel()} ${Obj.COOLDOWN} 1")

			// One gated call instead of five identical selector sweeps. See fire for why the reset
			// has to happen before the block is placed.
			executeRun("as ${block.markerSel("scores={${Obj.COOLDOWN}=20..}")} at @s if block ~ ~ ~ dispenser[triggered=true] if block ^ ^ ^1 air if data block ~ ~ ~ Items[0]", fire)
		}
	}

	/**
	 * Place one block in front. As the marker, at the placer, already confirmed powered, with air
	 * in front and something in the inventory.
	 *
	 * The cooldown is reset before anything is placed, for the same reason the Block Breaker
	 * resets first: the old version placed the block and then re-tested `if block ^ ^ ^1 air`
	 * before resetting. Placing a block makes that space not air, so the reset never ran, the
	 * counter climbed for ever, and the placer emptied itself at one block per tick.
	 */
	private fun Function.fire() {
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)
		raw("data modify storage ra:temp place_item set from block ~ ~ ~ Items[0]")
		executeRun("positioned ^ ^ ^1", placeBlockFn, "with storage ra:temp place_item")
		raw("playsound minecraft:block.stone.place block ${soundListenersSel()} ^ ^ ^1 1 0.8")
		raw("particle minecraft:cloud ^ ^ ^1 0.2 0.2 0.2 0.02 5 normal ${particleViewersSel()}")
	}

	/**
	 * Macro `{id, count}`: place the block here (the cell in front) and take one from the stack
	 * behind. Named apart from core `placeBlock(block)`, the placement-handler step.
	 */
	private fun Function.placeFromMacro() {
		raw("\$setblock ~ ~ ~ \$(id)")
		// Decrement the item count by 1 at the dispenser (^ ^ ^-1 from here).
		raw("\$scoreboard players set @s ${Obj.TEMP} \$(count)")
		scoreboard.players.remove(self(), Obj.TEMP, 1)
		raw("execute if score @s ${Obj.TEMP} matches 1.. positioned ^ ^ ^-1 store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get @s ${Obj.TEMP}")
		raw("execute if score @s ${Obj.TEMP} matches ..0 positioned ^ ^ ^-1 run data remove block ~ ~ ~ Items[0]")
	}
}
