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
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/**
 * Block Breaker: a dispenser that breaks the block in front of it when powered, at most once a
 * second (`ra.cooldown`, shared with ra_lib).
 */
object BlockBreaker {
	val block = CustomBlock(Ns.INTERACTIVE, "block_breaker", "Block Breaker", model = "minecraft:dispenser", dirType = 2)
	val item = block.loreItem {}
	val registerBlock = block.fn("register_block")
	private val fire = block.fn("fire")

	fun Ra.registerBlockBreaker() {
		block(block)
		// The cooldown is the block's own clock, not a setting.
		properties(block) {
			number("cooldown", min = 1, readonly = true)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.DISPENSER), count = null) {
			pattern("CCC", "CDP", "CCC")
			key("C", listOf(Items.COBBLESTONE, Items.BLACKSTONE))
			key("P", listOf(Items.STONE_PICKAXE))
			key("D", listOf(Items.DISPENSER))
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(fire) { fire() }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)

		pack.define(block.tick) {
			breakCheck(block, Blocks.DISPENSER, onBreak)

			raw("scoreboard players add ${block.markerSel()} ${Obj.COOLDOWN} 1")

			// One gated call instead of five identical selector sweeps that each re-tested the
			// same conditions -- and re-tested them AFTER the first had already changed the
			// world. See fire.
			executeRun("as ${block.markerSel("scores={${Obj.COOLDOWN}=20..}")} at @s if block ~ ~ ~ dispenser[triggered=true] unless block ^ ^ ^1 #air", fire)
		}
	}

	/**
	 * Break the block in front. As the marker, at the breaker, already confirmed powered and with
	 * something breakable in front.
	 *
	 * THE COOLDOWN IS RESET FIRST, AND THAT IS THE WHOLE POINT. This used to be five separate
	 * `execute ... unless block ^ ^ ^1 #air ...` lines, with the reset on the last one. The first
	 * line destroys that block -- so by the time the reset line re-tested
	 * `unless block ^ ^ ^1 #air`, the space WAS air, the condition was false, and the reset never
	 * ran. The counter climbed for ever, the `scores={ra.cooldown=20..}` gate was true on every
	 * tick from then on, and the breaker had no cooldown at all.
	 *
	 * Testing a condition once, acting on it, and never re-testing it afterwards is the fix. It is
	 * the same mistake the Clock's latch made in miniature.
	 */
	private fun Function.fire() {
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)
		raw("execute positioned ^ ^ ^1 unless block ~ ~ ~ #ra_lib:unbreakable run fill ~ ~ ~ ~ ~ ~ air destroy")
		raw("playsound minecraft:block.stone.break block ${soundListenersSel()} ~ ~ ~ 1 0.8")
		raw("""execute positioned ^ ^ ^1 run particle minecraft:block{block_state:"minecraft:stone"} ~ ~ ~ 0.3 0.3 0.3 0.1 20""")
	}
}
