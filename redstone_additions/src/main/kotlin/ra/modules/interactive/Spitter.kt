package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
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
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage
import ra.core.block.forEachMarker
import ra.core.block.placementGuard
import ra.core.block.dropItem
import ra.core.block.blockOnBreak
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/** Spitter: a dropper that throws its contents out in front, one item every 4 ticks. */
object Spitter {
	val block = CustomBlock(Ns.INTERACTIVE, "spitter", "Spitter", model = "minecraft:dropper", dirType = 2)
	val item = block.loreItem {}
	val registerBlock = block.fn("register_block")
	private val process = block.fn("process")

	private const val SPIT = "ra.spit"

	fun Ra.registerSpitter() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.DROPPER), count = null) {
			pattern("CCC", "RDR", "CCC")
			key("C", listOf(Items.COBBLESTONE, Items.BLACKSTONE))
			key("R", listOf(Items.REDSTONE))
			key("D", listOf(Items.DROPPER))
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// Dropper with full 6-directional facing.
			placeBlock(block)
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(process) { process() }

		val onBreak = pack.blockOnBreak(
			block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true,
			afterVanillaKill = { dropFirstSlot() },
		)

		pack.define(block.tick) {
			breakCheck(block, Blocks.DROPPER, onBreak)
			// Spit items if any in inventory.
			forEachMarker(block, process)
		}
	}

	/** One spitter. As its marker, at the dropper. Hopper rate: every 4 ticks. */
	private fun Function.process() {
		hopperCooldown()

		raw("execute unless data block ~ ~ ~ Items[0] run return 0")

		// Has an item: reset the cooldown and spit.
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)
		raw("data modify storage ra:temp spit_item set from block ~ ~ ~ Items[0]")

		// Summoned as stone, then given the real stack: an item entity cannot be summoned from a
		// stack held in storage.
		raw("""summon item ^ ^ ^0.8 {Item:{id:"minecraft:stone",count:1},Tags:["${Tag.RA}","$SPIT"],PickupDelay:10s}""")
		raw("data modify entity @e[type=item,tag=$SPIT,limit=1,sort=nearest] Item set from storage ra:temp spit_item")
		raw("tag @e[type=item,tag=$SPIT] remove $SPIT")

		raw("data remove block ~ ~ ~ Items[0]")
		raw("playsound minecraft:block.dispenser.dispense block ${soundListenersSel()} ~ ~ ~ 0.5 1.2")
	}
}
