package ra.modules.automation.crafter

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Obj
import ra.core.PropType
import ra.core.Ra
import ra.core.RA_CHAT_PREFIX
import ra.core.TagRoute
import ra.core.WrenchCycle
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.core.Ns
import ra.modules.automation.crafter.CrafterControllerRun.registerCrafterControllerRun
import ra.modules.automation.crafter.CrafterControllerRun.startIfIdle
import ra.modules.automation.pattern.Pattern
import ra.core.block.WRENCH_READOUT
import ra.core.block.noticeOnly

/**
 * Crafter Controller: fires the vanilla crafter next to it once the crafter holds what its
 * Pattern says, then waits until the crafter's contents change.
 *
 * A target block. Placed against a crafter (above, below or beside it; the first found of up,
 * north, south, east, west, down is the one it runs). Right-click it with a recorded Pattern to
 * load it (one Pattern from a held stack); the Pattern it held comes back to you. A plain
 * wrench click gives the Pattern back, Shift+RMB with the wrench switches it off and on (`enabled`). Breaking it drops the Pattern.
 *
 * FILLING. With a container on its other side, opposite the crafter, it also fills the crafter's
 * empty slots from it, one craft at a time ([CrafterFill]). With none, it only compares and fires.
 *
 * FIRING. A crafter crafts on a rising redstone edge. The controller powers itself for
 * [CrafterControllerRun.PULSE_TICKS] ticks (`target[power=15]`, a state change of the same
 * block, which updates its neighbours) and that is the crafter's edge.
 *
 * DATA. `data.properties.pattern`: the loaded Pattern's nine ids (read-only, shown by the Data
 * Handler). `data.data.item`: the Pattern itself, given back on eject or break.
 * `data.data.match` is the pattern checked by `ra_lib:safe/check` and ready to paste,
 * `data.data.checked` the list it was built from, `data.data.last` the crafter's `Items` at the
 * last look. See [CrafterControllerRun] for the schedule.
 */
object CrafterController {
	val block = CustomBlock(Ns.AUTOMATION, "crafter_controller", "Crafter Controller", model = "minecraft:target")
	val item = block.loreItem {
		lore(
			loreLine("Fires the crafter next to it", "gray"),
			loreLine("once it holds what its Pattern says", "gray"),
			loreLine("Right-click it with a Pattern to load one", "dark_gray"),
		)
	}

	val registerBlock = block.fn("register_block")
	/** As the marker at the block, the player tagged [Pattern.USER] holding a Pattern. */
	val insert = block.fn("insert")
	/** Wrench (plain RMB): the Pattern back out. As the marker, at the block. */
	val eject = block.fn("eject")
	private val cycleEnabled = block.fn("cycle_enabled")
	/** As the marker: drop `data.data.item` at `~ ~0.7 ~` and forget it. The item keeps [OUT]: the caller removes it ([untagOut]). */
	private val dropPattern = block.fn("drop_pattern")

	/** Item entities this block just spawned, before their stack is filled in. */
	private const val OUT = "ra.auto.out"
	private const val T = Obj.TEMP
	private const val CC = CrafterControllerRun.STORE
	private const val USER = "@a[tag=${Pattern.USER},limit=1]"
	private const val untagOut = "tag @e[type=minecraft:item,tag=$OUT] remove $OUT"

	fun Ra.registerCrafterController() {
		block(block)
		properties(block) {
			bool("enabled", default = true)
			list("pattern", of = PropType.ITEM, max = 9, pad = true, readonly = true, goggles = false, default = Pattern.blankSlotsSnbt)
		}
		wrenchCycle(block.id, WrenchCycle("Enabled", "enabled", cycleEnabled))
		wrenchToggles += TagRoute(block.tag, eject)
		Pattern.targets += TagRoute(block.tag, insert)
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.CRAFTER)) {
			pattern("IRI", "TCT", "III")
			key("I", Items.IRON_INGOT)
			key("R", Items.REDSTONE)
			key("T", Items.TARGET)
			key("C", Items.CRAFTER)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", "{enabled:1b,pattern:${Pattern.blankSlotsSnbt}}")
			setOnNew(block, "data.status", """{state:"no pattern",crafts:0}""")
			finishPlacement(block)
		}

		pack.define(insert) { insert() }
		pack.define(eject) {
			raw("""execute unless data entity @s data.data.item run return run tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"no Pattern loaded",color:"yellow"}]""")
			raw("function ${dropPattern.asId()}")
			raw(untagOut)
			raw("function ${CrafterControllerRun.arm.asId()}")
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"Pattern taken out",color:"aqua"}]""")
			raw("playsound minecraft:item.book.page_turn block ${soundListenersSel(10)} ~ ~ ~ 0.7 1.0")
		}
		pack.define(dropPattern) {
			raw("""summon minecraft:item ~ ~0.7 ~ {Item:{id:"minecraft:paper",count:1},Tags:["${OUT}"],PickupDelay:10s}""")
			raw("data modify entity @n[type=minecraft:item,tag=$OUT,distance=..1.5] Item set from entity @s data.data.item")
			raw("data remove entity @s data.data.item")
			raw("data modify entity @s data.properties.pattern set value ${Pattern.blankSlotsSnbt}")
		}
		pack.define(cycleEnabled) {
			// Read first, then write: the second test must not see the first one's write.
			raw("execute store success score #cc.on $T if data entity @s data.properties{enabled:0b}")
			raw("execute if score #cc.on $T matches 1 run data modify entity @s data.properties.enabled set value 1b")
			raw("execute if score #cc.on $T matches 0 run data modify entity @s data.properties.enabled set value 0b")
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{nbt:"data.properties.enabled",entity:"@s",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedStatusLine("crafts", "Crafts: ", "yellow")
			stackedTextLine("", "Right-click with a Pattern to load it", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true) {
			raw("execute if data entity @s data.data.item run function ${dropPattern.asId()}")
			raw(untagOut)
		}
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.TARGET)) { startIfIdle() }

		registerCrafterControllerRun()
	}

	/** As the marker at the block, the player tagged [Pattern.USER]: swap their Pattern for ours. */
	private fun Function.insert() {
		raw("scoreboard players set #pat.hit $T 1")
		raw("data modify storage $CC new set from entity $USER SelectedItem")
		raw("data modify storage $CC slots set from storage $CC new.components.\"minecraft:custom_data\".ra.pattern.slots")
		raw("execute store result score #cc.n $T run function ${CrafterControllerRun.count.asId()}")
		raw("execute if score #cc.n $T matches ..0 run data remove storage $CC new")
		raw("""execute if score #cc.n $T matches ..0 run return run tellraw $USER [$RA_CHAT_PREFIX,{text:"This Pattern is blank: sneak + right-click a crafter to record one.",color:"red"}]""")

		// The Pattern it held goes back to the player, at their feet with no pickup delay, theirs only.
		raw("execute if data entity @s data.data.item at $USER run function ${dropPattern.asId()}")
		raw("data modify entity @e[type=minecraft:item,tag=$OUT,limit=1] PickupDelay set value 0s")
		raw("data modify entity @e[type=minecraft:item,tag=$OUT,limit=1] Owner set from entity $USER UUID")
		raw(untagOut)
		// One Pattern of a held stack: the rest stays in the hand.
		raw("item modify entity $USER weapon.mainhand ${Pattern.TAKE_ONE}")

		raw("data modify entity @s data.data.item set from storage $CC new")
		raw("data modify entity @s data.data.item.count set value 1")
		raw("data modify entity @s data.properties.pattern set from storage $CC slots")
		raw("data remove storage $CC new")
		raw("function ${CrafterControllerRun.arm.asId()}")
		raw("""tellraw ${noticeOnly(USER)} [$RA_CHAT_PREFIX,{text:"Pattern loaded into the Crafter Controller.",color:"green"},{text:" A container on its other side fills the crafter.",color:"gray"}]""")
		raw("playsound minecraft:ui.cartography_table.take_result block ${soundListenersSel(10)} ~ ~ ~ 0.7 1.0")
	}
}
