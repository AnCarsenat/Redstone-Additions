package ra.modules.gates

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.features.predicates.providers.add
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.max
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.sub
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockRecipe
import ra.core.block.blockTick
import ra.core.block.dataLine
import ra.core.block.finishPlacement
import ra.core.block.forEachMarker
import ra.core.block.markerSel
import ra.core.block.gogglesHeader
import ra.core.block.newMarker
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.soundListenersSel
import ra.core.blockRegisterMessage
import ra.core.computeScore
import ra.core.define
import ra.core.provider
import ra.core.raw
import ra.core.scoreOf
import ra.lib.redstone.Side
import ra.lib.redstone.SideReaders

/**
 * Signal Calculator: maths on two signal levels, with an analog 0-15 output.
 *
 * - A: the strongest level on the bottom and the four sides. B: the level on the TOP (the same
 *   "top is the other input" convention as the Counter's reset). A repeater or comparator
 *   pointing in reads as 15; dust carries its real level.
 * - Output: every target block in the 3x3x3 cube gets the result as its power (see
 *   [analogShell]); iron blocks in the cube turn to redstone blocks while the result is above 0.
 * - `operation`, cycled by the wrench: add (capped at 15), subtract (floored at 0), max, min,
 *   greater / equal / less (15 when A > B, A = B, A < B, else 0), invert (15 - A, B unused).
 *
 * COST. Each side is first asked `any_side` (stops at the first source); only a powered side has
 * its level resolved. The levels are folded into a key (A * 16 + B) kept on the marker's
 * `ra.power` score, which this block never uses for anything else; the result is only worked out
 * and written when the key changes. An unpowered Calculator therefore costs six `any_side` reads,
 * like any other gate. Every 40 ticks the result is rewritten once, which picks up an operation
 * typed into the Data Handler, a target block placed into the cube since, and a hand-edited shell.
 */
object SignalCalculator {
	val block = CustomBlock(Ns.GATES, "signal_calculator", "Signal Calculator", model = "minecraft:quartz_bricks")
	val process = block.fn("process")
	private val update = block.fn("update")
	val cycleOperation = block.fn("cycle_operation")

	/** In cycle order; the last wraps to the first. Anything else in `operation` reads as "add". */
	private val OPERATIONS = listOf(
		"add" to "green", "subtract" to "red", "max" to "aqua", "min" to "dark_aqua",
		"greater" to "gold", "equal" to "yellow", "less" to "light_purple", "invert" to "gray",
	)

	/** The sides that make up A; the top is B. */
	private val A_SIDES = listOf(Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST, Side.DOWN)

	/** The result is known to match the blocks in the world. Cleared every [RESYNC] ticks. */
	private const val SYNCED = "ra.calc.synced"

	/** The iron shell is lit. */
	private const val LIT = "ra.calc.lit"

	private const val RESYNC = 40

	private const val T = Obj.TEMP
	private const val A = "#calc.a"
	private const val B = "#calc.b"
	private const val L = "#calc.l"
	private const val K = "#calc.k"
	private const val R = "#calc.r"

	/** The key of the inputs last worked out, on the marker. See the class comment. */
	private const val LAST_KEY = Obj.POWER

	private fun any(s: Side) = block.fn("any_${s.id}")
	private fun level(s: Side) = block.fn("level_${s.id}")

	fun Ra.registerSignalCalculator() {
		block(block)
		properties(block) {
			text("operation", default = "add")
		}
		wrenchCycle(block.id, WrenchCycle("Operation", "operation", cycleOperation))
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.COMPARATOR) {
			pattern(" T ", "CQC")
			key("T", Items.TARGET)
			key("C", Items.COMPARATOR)
			key("Q", Items.QUARTZ_BRICKS)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		for (s in A_SIDES + Side.UP) {
			pack.define(any(s)) { returnRun { function(SideReaders.anySide, s.args()) } }
			// The level into #calc.l, then folded into A (the max of its sides) or B.
			val into = if (s == Side.UP) B else A
			pack.define(level(s)) {
				execute {
					storeResult { score(literal(L), T) }
					run { function(RaLib.Redstone.side, s.args()) }
				}
				raw("scoreboard players operation $into $T > $L $T")
			}
		}
		pack.define(update) { update() }
		pack.define(cycleOperation) { cycleOperation() }
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("operation", "Operation: ", "aqua", y = 0.8)
			dataLine("a", "A: ", "yellow", y = 0.6)
			dataLine("b", "B (top): ", "yellow", y = 0.4)
			dataLine("level", "Output: ", "red", y = 0.2)
		}
		val onBreak = pack.analogOnBreak(block, "minecraft:block.stone.break")
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.QUARTZ_BRICKS)) {
			// The periodic rewrite: one counter for every Calculator, as the UNI Gate does.
			raw("scoreboard players add #calc.resync $T 1")
			raw("execute if score #calc.resync $T matches $RESYNC.. run tag ${block.markerSel()} remove $SYNCED")
			raw("execute if score #calc.resync $T matches $RESYNC.. run scoreboard players set #calc.resync $T 0")
			forEachMarker(block, process)
		}
	}

	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		data(newMarker(block)) {
			modify("data.properties.operation") { set("add") }
		}
		finishPlacement(block)
	}

	/** As the marker, at its position. */
	private fun Function.process() {
		raw("scoreboard players set $A $T 0")
		raw("scoreboard players set $B $T 0")

		// Read with the redstone_block shell ignored, so a lit output does not read back as input.
		raw("tag @s add $IGNORE_BLOCKS")
		for (s in A_SIDES + Side.UP) raw("execute if function ${any(s).asId()} run function ${level(s).asId()}")
		raw("tag @s remove $IGNORE_BLOCKS")

		// 16 is superpower (a repeater or comparator pointing in): a full signal here.
		raw("execute if score $A $T matches 16.. run scoreboard players set $A $T 15")
		raw("execute if score $B $T matches 16.. run scoreboard players set $B $T 15")

		raw("scoreboard players set #16 $T 16")
		raw("scoreboard players operation $K $T = $A $T")
		raw("scoreboard players operation $K $T *= #16 $T")
		raw("scoreboard players operation $K $T += $B $T")

		// Nothing changed and nothing asked for a rewrite: done.
		raw("execute if entity @s[tag=$SYNCED] if score @s $LAST_KEY = $K $T run return 0")
		raw("scoreboard players operation @s $LAST_KEY = $K $T")
		function(update)
	}

	/** Work out the result from #calc.a / #calc.b and write it out. As the marker, at it. */
	private fun Function.update() {
		fun op(name: String) = """data.properties{operation:"$name"}"""
		val a = scoreOf(A, T)
		val b = scoreOf(B, T)

		// add, and the fallback for a missing or unknown operation.
		computeScore(R, T, provider { min(add(a, b), constant(15)) })
		computeScore(R, T, provider { max(sub(a, b), constant(0)) }, chain = "if data entity @s ${op("subtract")}")
		computeScore(R, T, provider { max(a, b) }, chain = "if data entity @s ${op("max")}")
		computeScore(R, T, provider { min(a, b) }, chain = "if data entity @s ${op("min")}")
		computeScore(R, T, provider { sub(constant(15), a) }, chain = "if data entity @s ${op("invert")}")
		for ((name, test) in listOf("greater" to ">", "equal" to "=", "less" to "<")) {
			raw("execute if data entity @s ${op(name)} run scoreboard players set $R $T 0")
			raw("execute if data entity @s ${op(name)} if score $A $T $test $B $T run scoreboard players set $R $T 15")
		}

		raw("execute store result entity @s data.status.a int 1 run scoreboard players get $A $T")
		raw("execute store result entity @s data.status.b int 1 run scoreboard players get $B $T")
		raw("execute store result entity @s data.status.level int 1 run scoreboard players get $R $T")
		analogShell(R)

		// The iron shell: on while the result is above 0. Unsynced, it is written either way.
		raw("execute if score $R $T matches 1.. if entity @s[tag=!$LIT] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute if score $R $T matches 0 if entity @s[tag=$LIT] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		raw("execute if score $R $T matches 1.. if entity @s[tag=!$SYNCED] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute if score $R $T matches 0 if entity @s[tag=!$SYNCED] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		raw("execute if score $R $T matches 1.. run tag @s add $LIT")
		raw("execute if score $R $T matches 0 run tag @s remove $LIT")
		raw("tag @s add $SYNCED")
	}

	/** Wrench: add -> subtract -> ... -> invert -> add, and the output follows at once. As the marker, at it. */
	private fun Function.cycleOperation() {
		val m = "#calc.op $T"
		raw("scoreboard players set $m 0")
		OPERATIONS.forEachIndexed { i, (name, _) ->
			if (i > 0) raw("""execute if data entity @s data.properties{operation:"$name"} run scoreboard players set $m $i""")
		}
		OPERATIONS.forEachIndexed { i, _ ->
			val (next, color) = OPERATIONS[(i + 1) % OPERATIONS.size]
			raw("""execute if score $m matches $i run data modify entity @s data.properties.operation set value "$next"""")
			raw("""execute if score $m matches $i run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"$next",color:"$color"}]""")
		}
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		// The next process rewrites the output with the new operation.
		raw("tag @s remove $SYNCED")
	}
}
