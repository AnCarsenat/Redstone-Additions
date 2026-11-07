package ra.modules.ender

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.stackReset
import ra.core.block.stackedPropLine
import ra.core.define
import ra.core.raw
import ra.modules.ender.link.Link
import ra.modules.infinite.breakCheck
import ra.modules.infinite.loreLine
import ra.modules.sensors.batGuard
import ra.modules.sensors.clearNew
import ra.modules.sensors.exec
import ra.modules.sensors.execReturn
import ra.modules.sensors.setOnNew

/**
 * Ender Item Vault: a real barrel whose stacks move to the vault on its channel. The vault's
 * contents are the barrel's contents, so a hopper, a player and an Item Pipe all interact with it
 * without any special case.
 *
 * Modes (wrench Shift+RMB cycles them):
 * - `link`: two-way, demand driven -- insert here and it goes there, take here and it comes back.
 *   For automation on both ends.
 * - `send`: one-way out.
 * - `receive`: one-way in.
 */
object ItemVault {
	val block = CustomBlock(Ns.ENDER, "ender_item_vault", "Ender Item Vault", model = "minecraft:barrel", dir = "item_vault")
	val item = block.enderItem(
		"rare",
		loreLine("Shares its contents with the vault on its channel", "gray"),
		loreLine("Shift+RMB with the wrench: link / send / receive", "dark_gray"),
	)

	val process = block.fn("process")
	val mark = block.fn("mark")
	val push = block.fn("push")
	val pull = block.fn("pull")
	val pushToPuller = block.fn("push_to_puller")
	val where = block.fn("where")
	val cycleMode = block.fn("cycle_mode")
	val registerBlock = block.fn("register_block")

	private const val T = Obj.TEMP

	fun Ra.registerItemVault() {
		block(block)
		wrenchCycle(block.id, WrenchCycle("Mode", "mode", cycleMode))
		pack.enderRegisterBlock(block)
		pack.enderGive(block, item, """{text:" — set the channel with the Data Handler",color:"gray"}""")
		pack.enderRecipe(block, item, RecipeCategory.MISC) {
			pattern(" P ", "PBP", "OOO")
			key("P", Items.ENDER_PEARL)
			key("B", Items.BARREL)
			key("O", Items.OBSIDIAN)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			batGuard(block)
			placeBlock(block)
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.properties.mode", "\"link\"")
			clearNew(block)
			raw("tag @s remove ${block.placeTag}")
			raw("return 1")
		}

		pack.define(cycleMode) { cycleMode() }
		pack.define(process) { process() }
		registerMoves()

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.25)
			stackReset(top = 105, step = 16)
			stackedPropLine("channel", "Channel: ", "light_purple")
			stackedPropLine("mode", "Mode: ", "aqua")
		}

		val onBreak = pack.enderOnBreak(block, item)
		pack.define(block.tick) { tick(onBreak) }
	}

	/** Cycle link -> send -> receive. As the vault marker, at the block. */
	private fun Function.cycleMode() {
		raw("scoreboard players set #ender.mode $T 0")
		raw("execute if data entity @s data.properties{mode:\"link\"} run scoreboard players set #ender.mode $T 1")
		raw("execute if data entity @s data.properties{mode:\"send\"} run scoreboard players set #ender.mode $T 2")
		raw("execute if data entity @s data.properties{mode:\"receive\"} run scoreboard players set #ender.mode $T 3")

		raw("execute if score #ender.mode $T matches 1 run data modify entity @s data.properties.mode set value \"send\"")
		raw("execute if score #ender.mode $T matches 2 run data modify entity @s data.properties.mode set value \"receive\"")
		raw("execute unless score #ender.mode $T matches 1..2 run data modify entity @s data.properties.mode set value \"link\"")

		raw("execute if score #ender.mode $T matches 1 run ${wrenchSay(block.name, "send", "green")}")
		raw("execute if score #ender.mode $T matches 2 run ${wrenchSay(block.name, "receive", "aqua")}")
		raw("execute unless score #ender.mode $T matches 1..2 run ${wrenchSay(block.name, "link (two-way)", "light_purple")}")
		raw(WRENCH_CLICK)
	}

	/**
	 * Move stacks between this vault and its partner. As a vault marker, at the barrel.
	 *
	 * `link` mode is two-way, and it works by watching what the *outside world* does to this
	 * barrel rather than by comparing the two ends. Every cycle the number of stacks present is
	 * compared with the number this vault last left behind:
	 *
	 *   more than before  -> something was put in here, so push a stack across
	 *   fewer than before -> something was taken out here, so pull one back
	 *   the same          -> nobody touched either end; do nothing at all
	 *
	 * Comparing contents instead would shuffle forever: A hands its only stack to B, which then
	 * has more than A and hands it straight back. And a rule that only moved on a difference of two
	 * or more could never move a single stack at all, which is what made the first version one-way.
	 *
	 * Deliveries update the receiver's mark immediately, so an arriving stack is not mistaken for
	 * an insert and bounced back.
	 */
	private fun Function.process() {
		raw("scoreboard players set @s ${EnderIds.CD} 0")

		// Held for the length of this call (see EnderIds.SELF).
		tag(self()) { add(EnderIds.SELF) }

		// How full the barrel is now, and how full it was when we last looked.
		raw("scoreboard players set #ender.used $T 0")
		raw("execute if data block ~ ~ ~ Items store result score #ender.used $T run data get block ~ ~ ~ Items")
		raw("execute unless data entity @s data.data.last_used run data modify entity @s data.data.last_used set value 0")
		raw("execute store result score #ender.was $T run data get entity @s data.data.last_used")

		// One-way modes keep their old, simpler behaviour: a sender drains, a receiver waits.
		exec("if data entity @s data.properties{mode:\"send\"}", push)
		exec("if data entity @s data.properties{mode:\"receive\"}", mark)
		execReturn("unless data entity @s data.properties{mode:\"link\"}", Link.done)

		// Two-way: follow the outside world.
		exec("if score #ender.used $T > #ender.was $T", push)
		exec("if score #ender.used $T < #ender.was $T", pull)

		function(mark)

		function(Link.done)
	}

	private fun Function.tick(onBreak: io.github.ayfri.kore.arguments.types.resources.FunctionArgument) {
		breakCheck(block, Blocks.BARREL, onBreak)

		// Defaults for vaults placed before a property existed.
		enderDefault(block, "channel", "\"default\"")
		// Migration: `shared` is gone. A vault still set to it becomes `link`, which is the closest
		// of the three that remain -- two-way and demand driven.
		raw("execute as @e[type=marker,tag=${block.tag}] if data entity @s data.properties{mode:\"shared\"} run data modify entity @s data.properties.mode set value \"link\"")
		enderDefault(block, "mode", "\"link\"")

		// The `ra.ender.occupied` and `ra.ender.share` bookkeeping went with `shared`: both existed
		// only to work out which vault a player was standing at. Two whole-world sweeps a tick, gone.
		raw("tag @e[type=marker,tag=${EnderIds.RECV_ITEM}] remove ${EnderIds.RECV_ITEM}")
		raw("tag @e[type=marker,tag=${EnderIds.SEND_ITEM}] remove ${EnderIds.SEND_ITEM}")

		tagUnlessMode(block, "send", EnderIds.RECV_ITEM)
		tagUnlessMode(block, "receive", EnderIds.SEND_ITEM)

		// Hopper rate: one stack every 4 ticks per sending vault.
		raw("scoreboard players add @e[type=marker,tag=${block.tag}] ${EnderIds.CD} 1")
		exec("as @e[type=marker,tag=${block.tag},scores={${EnderIds.CD}=4..}] at @s", process)
	}

	private fun Ra.registerMoves() {
		// Record how full the barrel is, so the next cycle can tell an outside change from one
		// this module made itself. As the vault marker, at the barrel.
		pack.define(mark) {
			raw("scoreboard players set #ender.mark $T 0")
			raw("execute if data block ~ ~ ~ Items store result score #ender.mark $T run data get block ~ ~ ~ Items")
			raw("execute store result entity @s data.data.last_used int 1 run scoreboard players get #ender.mark $T")
		}

		// Hand one stack to the partner on this channel. As the vault marker, at the barrel.
		pack.define(push) { pushOne(EnderIds.RECV_ITEM) }

		// Push one stack specifically to the vault that asked for it. As the giving vault marker.
		pack.define(pushToPuller) {
			pushOne(EnderIds.PULL_TARGET)
			// This end just lost a stack on purpose; record that so it is not read as an
			// extraction next cycle, which would have it pull the stack straight back.
			function(mark)
		}

		// Ask the partner for one stack, because something was taken out of this end. As the
		// vault marker, at the barrel.
		//
		// Rather than a second copy of the move logic with the roles swapped, this makes itself the
		// only eligible receiver for one command and runs the partner's push.
		pack.define(pull) {
			tag(self()) { add(EnderIds.PULL_TARGET) }
			linkRequest(EnderIds.PULL_TARGET)
			function(Link.requestItems, Storage.ENDER, "link")
			tag(self()) { remove(EnderIds.PULL_TARGET) }
		}

		// Write this barrel's block coordinates into storage ra:ender move. As the vault marker.
		//
		// `data get Pos[0] 1` truncates toward zero, so at x = -10.5 it reports -10 while the block
		// is -11 -- and that block is the one whose slot gets cleared after the copy. Reading
		// thousandths and dividing gets it right at negative coordinates too, because scoreboard
		// division rounds down.
		pack.define(where) {
			raw("scoreboard players set #ender.k $T 1000")
			for ((i, axis) in listOf("x", "y", "z").withIndex())
				raw("execute store result score #ender.$axis $T run data get entity @s Pos[$i] 1000")
			for (axis in listOf("x", "y", "z"))
				raw("scoreboard players operation #ender.$axis $T /= #ender.k $T")

			raw("data modify storage ra:ender move set value {}")
			for (axis in listOf("x", "y", "z"))
				raw("execute store result storage ra:ender move.$axis int 1 run scoreboard players get #ender.$axis $T")
		}
	}

	/** `storage ra:ender link = {channel: <ours>, recv: <recv>}`. */
	private fun Function.linkRequest(recv: String) {
		raw("data modify storage ra:ender link set value {}")
		raw("data modify storage ra:ender link.channel set from entity @s data.properties.channel")
		raw("data modify storage ra:ender link.recv set value \"$recv\"")
	}

	/** One stack from this barrel to the nearest vault on the channel wearing [recv]. */
	private fun Function.pushOne(recv: String) {
		raw("execute unless data block ~ ~ ~ Items[0] run return 0")

		function(where)
		raw("execute store result storage ra:ender move.src_slot int 1 run data get block ~ ~ ~ Items[0].Slot")

		linkRequest(recv)
		tag(self()) { add(EnderIds.SENDING) }
		function(Link.sendItems, Storage.ENDER, "link")
		tag(self()) { remove(EnderIds.SENDING) }
	}
}
