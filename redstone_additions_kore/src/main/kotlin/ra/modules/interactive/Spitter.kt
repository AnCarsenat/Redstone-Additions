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
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.modules.infinite.breakCheck
import ra.modules.infinite.finishLorePlacement
import ra.core.item.loreItem
import ra.modules.infinite.registerBlockMessage

/** Spitter: a dropper that throws its contents out in front, one item every 4 ticks. */
object Spitter {
	val block = CustomBlock(Ns.INTERACTIVE, "spitter", "Spitter", model = "minecraft:dropper", dirType = 2)
	val item = block.loreItem {}
	val registerBlock = block.fn("register_block")
	private val process = block.fn("process")

	private const val SPIT = "ra.spit"

	fun Ra.registerSpitter() {
		block(block)
		pack.registerBlockMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(
			block.ns.id, block.id, item,
			pattern = listOf("CCC", "RDR", "CCC"),
			keys = listOf("C" to listOf(Items.COBBLESTONE, Items.BLACKSTONE), "R" to listOf(Items.REDSTONE), "D" to listOf(Items.DROPPER)),
			unlock = has(Items.DROPPER),
		)

		pack.define(block.handlePlacement) {
			placeGuard(block)
			// Dropper with full 6-directional facing.
			placeBlock(block)
			finishLorePlacement(block)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(process) { process() }

		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropFirstSlot()
			dropSpec(item)
			breakSound()
			breakCloud()
			kill(self())
		}

		pack.define(block.tick) {
			breakCheck(block, Blocks.DROPPER, onBreak)
			// Spit items if any in inventory.
			forEachMarker(block, process)
		}
	}

	/** One spitter. As its marker, at the dropper. */
	private fun Function.process() {
		raw("execute unless score @s ${Obj.COOLDOWN} matches -2147483648.. run scoreboard players set @s ${Obj.COOLDOWN} 0")
		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute if score @s ${Obj.COOLDOWN} matches ..3 run return 0")

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
		raw("playsound minecraft:block.dispenser.dispense block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.2")
	}
}
