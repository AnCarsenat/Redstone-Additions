package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.modules.infinite.breakCheck
import ra.modules.infinite.finishLorePlacement
import ra.modules.infinite.loreItem
import ra.modules.infinite.registerBlockMessage

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
		readonly(block.id, "cooldown")
		pack.registerBlockMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(
			block.ns.id, block.id, item,
			pattern = listOf("CCC", "CDP", "CCC"),
			keys = listOf("C" to listOf(Items.COBBLESTONE, Items.BLACKSTONE), "P" to listOf(Items.STONE_PICKAXE), "D" to listOf(Items.DISPENSER)),
			unlock = has(Items.DISPENSER),
		)

		pack.define(block.handlePlacement) {
			placeGuard(block)
			placeBlock(block)
			finishLorePlacement(block)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(fire) { fire() }

		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropSpec(item)
			breakSound()
			breakCloud()
			kill(self())
		}

		pack.define(block.tick) {
			breakCheck(block, Blocks.DISPENSER, onBreak)

			raw("scoreboard players add @e[type=marker,tag=${block.tag}] ${Obj.COOLDOWN} 1")

			// One gated call instead of five identical selector sweeps that each re-tested the
			// same conditions -- and re-tested them AFTER the first had already changed the
			// world. See fire.
			executeRun("as @e[type=marker,tag=${block.tag},scores={${Obj.COOLDOWN}=20..}] at @s if block ~ ~ ~ dispenser[triggered=true] unless block ^ ^ ^1 #air", fire)
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
		raw("playsound minecraft:block.stone.break block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 1 0.8")
		raw("""execute positioned ^ ^ ^1 run particle minecraft:block{block_state:"minecraft:stone"} ~ ~ ~ 0.3 0.3 0.3 0.1 20""")
	}
}
