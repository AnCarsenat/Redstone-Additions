package ra.modules.enchanting.sacrifice

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.enchanting.EnchantingModule
import ra.core.block.noticeOnly

/** The `ra_enchanting:sacrifice` functions: one sacrificed stack, as the item entity, at it. */
object Sacrifice {
	val onTable = ref(Ns.ENCHANTING, "sacrifice/on_table")
	val attempt = ref(Ns.ENCHANTING, "sacrifice/attempt")
	val success = ref(Ns.ENCHANTING, "sacrifice/success")
	val fail = ref(Ns.ENCHANTING, "sacrifice/fail")

	fun Ra.registerSacrifice() {
		pack.define(onTable) { onTable() }
		pack.define(attempt) { attempt() }
		pack.define(success) { success() }
		pack.define(fail) { fail() }
	}

	/** One sacrificed stack, resting on an enchanting table. */
	private fun Function.onTable() {
		// Four scan steps = one second between rolls, so a stack burns down visibly instead of
		// vanishing in a single tick.
		scoreboard.players.add(self(), EnchantingModule.CD, 1)
		raw("execute unless score @s ${EnchantingModule.CD} matches 4.. run return 0")
		scoreboard.players.set(self(), EnchantingModule.CD, 0)

		data(Storage.ENCHANT) { modify("input") { set(self(), "Item") } }
		data(Storage.ENCHANT) { remove("result") }
		data(Storage.ENCHANT) { remove("chance") }
		data(Storage.ENCHANT) { remove("won") }
		data(Storage.ENCHANT) { remove("lost") }
		function(EnchantingModule.match)

		// Nothing wants this item -- leave the stack alone.
		raw("execute unless data storage ra:enchant result run return 0")

		function(attempt)
	}

	/**
	 * Burn one item off the stack and roll for the upgrade.
	 * Input: storage ra:enchant result (item to produce) and chance (1..100).
	 */
	private fun Function.attempt() {
		val roll = literal("#ench.roll")
		val chance = literal("#ench.chance")
		val count = literal("#ench.count")

		raw("execute store result score #ench.roll ${Obj.TEMP} run random value 1..100")
		raw("execute store result score #ench.chance ${Obj.TEMP} run data get storage ra:enchant chance 1")

		// Consume exactly one item. The entity is killed after the effects run so the success item
		// still has a position to spawn at.
		raw("execute store result score #ench.count ${Obj.TEMP} run data get entity @s Item.count")
		scoreboard.players.remove(count, Obj.TEMP, 1)
		raw("execute if score #ench.count ${Obj.TEMP} matches 1.. store result entity @s Item.count int 1 run scoreboard players get #ench.count ${Obj.TEMP}")

		execute {
			ifCondition { score(roll, Obj.TEMP) lessThanOrEqualTo score(chance, Obj.TEMP) }
			run { function(success) }
		}
		execute {
			unlessCondition { score(roll, Obj.TEMP) lessThanOrEqualTo score(chance, Obj.TEMP) }
			run { function(fail) }
		}

		execute {
			ifCondition { score(count, Obj.TEMP) lessThanOrEqualTo 0 }
			run { kill(self()) }
		}
	}

	/** The roll came up -- hand out the upgraded item. */
	private fun Function.success() {
		// ra.ench.done keeps the product out of the next scan, so an upgrade that landed back on the
		// same table is not immediately sacrificed again.
		raw("""summon item ~ ~0.3 ~ {Item:{id:"minecraft:stone",count:1},Tags:["ra","ra.ench.product","${EnchantingModule.DONE}"],PickupDelay:20s}""")
		raw("data modify entity @e[type=item,tag=ra.ench.product,distance=..0.6,limit=1,sort=nearest] Item set from storage ra:enchant result")
		raw("tag @e[type=item,tag=ra.ench.product,distance=..0.6,limit=1,sort=nearest] remove ra.ench.product")

		raw("particle minecraft:enchant ~ ~0.6 ~ 0.3 0.4 0.3 0.8 60 normal ${particleViewersSel()}")
		raw("particle minecraft:end_rod ~ ~0.4 ~ 0.15 0.15 0.15 0.05 12 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.enchantment_table.use block ${soundListenersSel()} ~ ~ ~ 1 1.2")
		raw("playsound minecraft:entity.player.levelup block ${soundListenersSel()} ~ ~ ~ 0.8 1.4")
		tellThrower("won", "light_purple")
	}

	/** The roll missed -- the item is gone, nothing comes back. */
	private fun Function.fail() {
		raw("particle minecraft:lava ~ ~0.4 ~ 0.2 0.2 0.2 0.05 10 normal ${particleViewersSel()}")
		raw("particle minecraft:smoke ~ ~0.4 ~ 0.2 0.2 0.2 0.02 12 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.fire.extinguish block ${soundListenersSel()} ~ ~ ~ 1 0.6")
		raw("playsound minecraft:entity.item.break block ${soundListenersSel()} ~ ~ ~ 0.8 0.8")
		tellThrower("lost", "gray")
	}

	/**
	 * The recipe's message ([ra.modules.enchanting.sacrificeRecipe] `won` / `lost`), if it has one,
	 * to the player who threw the item. `on origin` is an item's Thrower; an item dropped by a
	 * hopper or a mob, or thrown by a player who has left, has none and nobody is told.
	 */
	private fun Function.tellThrower(key: String, color: String) {
		raw("""execute if data storage ra:enchant $key on origin run tellraw ${noticeOnly("@s[type=player]")} [{text:"[RA] ",color:"gold"},{storage:"ra:enchant",nbt:"$key",color:"$color"}]""")
	}
}
