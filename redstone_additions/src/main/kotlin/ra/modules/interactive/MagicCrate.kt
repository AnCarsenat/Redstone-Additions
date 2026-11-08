package ra.modules.interactive

import ra.core.computeInto
import ra.core.provider
import ra.core.scoreOf
import ra.core.storeInt
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.predicates.providers.min
import ra.core.Storage
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.stackedDataLine
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.item.loreLine
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage
import ra.core.executeStoreRun
import ra.core.callWith
import ra.core.macroCall
import ra.core.ensureScore
import ra.core.block.forEachMarker
import ra.core.block.placementGuard
import ra.core.block.dropItem
import ra.core.block.blockOnBreak
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.particleViewersSel
import ra.core.block.readProperty
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/** Magic Crate: a barrel that pulls dropped items in from around it, every `cooldown` ticks. */
object MagicCrate {
	val block = CustomBlock(Ns.INTERACTIVE, "magic_crate", "Magic Crate", model = "minecraft:barrel")
	val item = block.loreItem { lore(loreLine("Pulls dropped items in from up to 20 blocks", "gray")) }
	val registerBlock = block.fn("register_block")
	private val pull = block.fn("pull")
	private val sweep = block.fn("sweep")
	private val tickOne = block.fn("tick_one")


	fun Ra.registerMagicCrate() {
		block(block)
		properties(block) {
			// The same ceiling tick_one clamps to: a schema that allowed 32 let players type a
			// radius the crate silently ignored.
			number("radius", min = 5, max = 20)
			// A server admin's setting (a fast pulse is a server cost), and not what you look at a
			// crate to learn: the Goggles show its state, radius and last pulse instead.
			number("cooldown", min = 1, max = 1200, adminOnly = true, goggles = false)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.ENDER_PEARL), count = null) {
			pattern("EPE", "IHI", "ECE")
			key("E", listOf(Items.ENDER_PEARL))
			key("P", listOf(Items.ENDER_EYE))
			key("I", listOf(Items.IRON_INGOT))
			key("H", listOf(Items.HOPPER))
			key("C", listOf(Items.CHEST))
		}

		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(block.goggles) { goggles() }
		pack.define(pull) { pull() }
		pack.define(sweep) { sweep() }
		pack.define(tickOne) { tickOne() }

		// The vanilla drop goes, and the custom item is summoned in its place. This ran bare, so it
		// killed whatever barrel happened to be within two blocks of wherever the tick function
		// stood -- never the one that had just dropped out of this block. Breaking one handed back
		// both items.
		val onBreak = pack.blockOnBreak(
			block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND,
		)

		pack.define(block.tick) {
			breakCheck(block, Blocks.BARREL, onBreak)
			forEachMarker(block, tickOne)
		}
	}

	/** As the spawned bat, at its position. */
	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		// dir_type 0: it reaches in every direction, so which way it was placed means nothing and a
		// facing would only be one more thing to get wrong.
		//
		// A plain barrel, with no skin over it. It wore a hopper for a while, which was a lie in two
		// directions: a hopper is five slots that push downwards, and this is twenty-seven slots
		// that pull inwards. A crate looks like what it is.
		placeBlock(block)
		raw("data modify entity @e[type=marker,tag=${block.tag},tag=${Tag.NEW},limit=1,sort=nearest] data.properties set value {radius:8,cooldown:20}")
		finishPlacement(block, removePlaceTag = false, withinBlock = false)
	}

	private fun Function.goggles() {
		gogglesHeader(block, nameY = 1.0)
		stackedStatusLine("state", "", "aqua")
		stackedPropLine("radius", "Radius: ", "aqua", " blocks")
		stackedPropLine("cooldown", "Cooldown: ", "aqua", " ticks")
		stackedDataLine("pulled", "Last pulse: ", "green", " items")
	}

	/**
	 * Take one item entity into the crate. As the item, at the crate's block.
	 *
	 * The whole stack crosses verbatim by appending the item entity's own Item compound to the
	 * container's Items list. Rebuilding it from an id and a count through a loot table would drop
	 * the components, and a named, enchanted or damaged item would arrive as a plain one.
	 */
	private fun Function.pull() {
		raw("execute unless block ~ ~ ~ barrel run return 0")

		// An item still on pickup delay was just thrown by a player who has not let go of it yet --
		// a dropped stack teleporting back out of reach reads as the block eating your inventory.
		raw("execute if data entity @s {PickupDelay:32767s} run return 0")

		executeStoreRun("", "#mh.slot", RaLib.Inventory.findFreeSlot)
		raw("execute if score #mh.slot ${Obj.TEMP} matches ..-1 run return 0")

		raw("data modify storage ra:interactive mh.stack set from entity @s Item")
		raw("execute store result storage ra:interactive mh.stack.Slot byte 1 run scoreboard players get #mh.slot ${Obj.TEMP}")
		raw("data modify block ~ ~ ~ Items append from storage ra:interactive mh.stack")

		raw("execute at @s run particle minecraft:portal ~ ~0.2 ~ 0.2 0.2 0.2 0.05 12 normal ${particleViewersSel()}")
		raw("particle minecraft:enchant ~ ~1 ~ 0.3 0.3 0.3 0.4 8 normal ${particleViewersSel()}")
		raw("playsound minecraft:entity.enderman.teleport block ${soundListenersSel(12)} ~ ~ ~ 0.2 1.8")

		scoreboard.players.add(literal("#mh.pulled"), Obj.TEMP, 1)
		kill(self())
	}

	/**
	 * Macro `{r}`: reach out and take. As the marker, at the crate block.
	 *
	 * `execute as` changes who is running, not where it is running, so inside `pull` @s is the item
	 * while the position is still the crate -- which is exactly the pair of contexts the insert
	 * needs, with no position juggling.
	 *
	 * The limit caps the work one pulse can do. A crate standing in a mob farm's drop pile would
	 * otherwise walk every item entity in a 20 block sphere in a single tick; eight per pulse still
	 * clears a pile quickly and never spikes.
	 */
	private fun Function.sweep() {
		raw("\$execute as @e[type=item,distance=..\$(r),sort=nearest,limit=8] run ${macroCall(pull)}")
	}

	/**
	 * One crate's pulse. As its marker, at its block.
	 *
	 * THE COOLDOWN GUARD. A score that has never been set reads as ABSENT, and every comparison
	 * against an absent score is false -- including `matches 1..`, which is how a missing cooldown
	 * used to mean "fire this tick" and made the block run every tick regardless of what the
	 * player configured. [ensureScore] gives it a starting value first.
	 */
	private fun Function.tickOne() {
		ensureScore("@s", Obj.COOLDOWN)
		scoreboard.players.remove(self(), Obj.COOLDOWN, 1)
		raw("execute if score @s ${Obj.COOLDOWN} matches 1.. run return 0")

		readProperty("cooldown", default = 20, min = 1)
		raw("scoreboard players operation @s ${Obj.COOLDOWN} = #prop ${Obj.TEMP}")

		// Radius is clamped at both ends. The floor lives in util/property; the ceiling is here
		// because a radius the player could raise without limit would turn one block into a
		// server-wide entity selector.
		readProperty("radius", default = 8, min = 5)
		computeInto(storeInt(Storage.INTERACTIVE, "mh.r"), provider { min(scoreOf("#prop", Obj.TEMP), 20) })

		scoreboard.players.set(literal("#mh.pulled"), Obj.TEMP, 0)
		callWith(sweep, "storage ra:interactive mh")

		raw("execute store result entity @s data.data.pulled int 1 run scoreboard players get #mh.pulled ${Obj.TEMP}")
		raw("data modify entity @s data.status.radius set from entity @s data.properties.radius")

		executeStoreRun("", "#mh.free", RaLib.Inventory.hasFreeSlot)
		raw("execute if score #mh.free ${Obj.TEMP} matches 1.. run data modify entity @s data.status.state set value \"Ready\"")
		raw("execute if score #mh.free ${Obj.TEMP} matches ..0 run data modify entity @s data.status.state set value \"Full\"")
	}
}
