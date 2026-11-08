package ra.modules.gates

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockOnBreak
import ra.core.block.blockRecipe
import ra.core.block.blockTick
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import io.github.ayfri.kore.functions.Function
import ra.core.block.newMarker
import ra.core.define
import ra.core.raw
import ra.core.blockRegisterMessage
import ra.core.block.outputShell
import ra.core.block.forEachMarker
import ra.core.ensureScore

/**
 * Clock: pulses a ring of redstone for one tick every `cooldown` ticks.
 *
 * Physically a daylight detector with a small clock item laid on top; the "ring" is the 3x3x3
 * cube around it, where iron blocks are swapped for redstone blocks for the pulse.
 */
object Clock {
	val block = CustomBlock(Ns.GATES, "clock", "Clock", model = "minecraft:daylight_detector")

	/** Latch: the pulse fired last tick and must be put out this tick. */
	internal const val ON = "ra.clock_on"

	/** The floating clock item. */
	private const val DISPLAY_TAG = "ra.custom_block.display_item.clock"

	val process = block.fn("process")

	fun Ra.registerClock() {
		block(block)
		properties(block) {
			number("cooldown", min = 2, max = 1200)
			// The pre-5.1 name of cooldown, folded into it every tick.
			number("delay", readonly = true, visible = false)
		}
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.DAYLIGHT_DETECTOR) {
			pattern("RCR", "SSS")
			key("R", Items.REPEATER)
			key("C", Items.CLOCK)
			key("S", Items.STONE)
		}
		// Registration is a debug message only; kept so `/function ra_gates:load` stays verbose for ra.debug.
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.define(block.goggles) { goggles() }

		val onBreak = pack.blockOnBreak(block) {
			// The clock item and any pulse still lit. A display left behind floats forever, and a
			// lit ring keeps powering whatever it touched.
			kill(allEntities { type = EntityTypes.ITEM_DISPLAY; tag = block.tag; distance = rangeOrDoubleEnd(2.0) })
			kill(allEntities { type = EntityTypes.ITEM_DISPLAY; tag = DISPLAY_TAG; distance = rangeOrDoubleEnd(2.0) })
			outputShell(on = false)
			tag(self()) { remove(ON) }
		}

		pack.blockTick(block, onBreak) {
			// An old clock's `delay` becomes `cooldown` in the 5.1.7 migration, not here.
			// No macro call: `with entity @s data.properties` fails outright on a clock with no
			// `cooldown`, which is every clock placed before it existed, and that clock never ticks.
			forEachMarker(block, process)
		}
	}

	/** As the spawned bat, at the block centre. Returns 1 when it placed a clock. */
	private fun Function.handlePlacement() {
		placementGuard(block)
		placeBlock(block)

		// Display offsets are measured from the marker, which sits at the BLOCK CENTRE (y + 0.5).
		// The daylight detector's top face is at 0.375, so the clock rests at -0.125 from the
		// centre, plus a hair to keep it out of the surface.
		kill(allEntities { type = EntityTypes.ITEM_DISPLAY; tag = block.tag; distance = rangeOrDoubleEnd(0.6) })
		kill(allEntities { type = EntityTypes.ITEM_DISPLAY; tag = DISPLAY_TAG; distance = rangeOrDoubleEnd(0.6) })
		raw("""summon item_display ~ ~-0.115 ~ {item:{id:"minecraft:clock",count:1},item_display:"fixed",transformation:{left_rotation:[0.7071f,0f,0f,0.7071f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]},Tags:["ra","ra.custom_block.display_item","$DISPLAY_TAG"]}""")

		// One property, `cooldown`, in ticks. Anything the item carried is discarded so a fresh
		// clock never starts life with two names for the same number.
		val fresh = newMarker(block)
		data(fresh) {
			modify("data.properties.cooldown") { set(20) }
			remove("data.properties.delay")
		}
		scoreboard.players.set(fresh, Obj.COOLDOWN, 0)

		finishPlacement(block)
	}

	/**
	 * Pulse: swap the ring to redstone for exactly one tick, every `cooldown` ticks.
	 * As the clock marker, at its position.
	 *
	 * THE SHAPE THAT MATTERS. The output is put out at the TOP of the tick, but only if it was
	 * lit -- never unconditionally. An unconditional off-fill followed by a conditional on-fill
	 * runs both on the same tick whenever the period is short, and the ring reads as permanently
	 * powered rather than pulsing. It also means an idle clock writes no blocks at all; the old
	 * version filled 27 blocks every tick of every clock in the world.
	 */
	private fun Function.process() {
		val was = literal("#clk.was")
		val max = literal("#clk.max")

		// Put out last tick's pulse, only if there is one.
		scoreboard.players.set(was, Obj.TEMP, 0)
		execute {
			ifCondition { entity(self { tag = ON }) }
			run { scoreboard.players.set(was, Obj.TEMP, 1) }
		}
		execute {
			ifCondition { score(was, Obj.TEMP) equalTo 1 }
			run { tag(self()) { remove(ON) } }
		}
		execute {
			ifCondition { score(was, Obj.TEMP) equalTo 1 }
			at(self())
			run { outputShell(on = false) }
		}

		// The period. Guarded, because `store result` writes ZERO when its command fails, and zero
		// means "fire every tick". Floored at 2: a pulse needs a tick on and a tick off; at 1 the
		// two fills meet on the same tick and the ring never goes dark.
		scoreboard.players.set(max, Obj.TEMP, 20)
		raw("execute if data entity @s data.properties.cooldown store result score #clk.max ${Obj.TEMP} run data get entity @s data.properties.cooldown 1")
		execute {
			ifCondition { score(max, Obj.TEMP) lessThanOrEqualTo 2 }
			run { scoreboard.players.set(max, Obj.TEMP, 2) }
		}

		// A score that was never set reads as absent, and comparing against an absent score is
		// false -- which would fire every tick.
		ensureScore("@s", Obj.COOLDOWN)

		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute if score @s ${Obj.COOLDOWN} < #clk.max ${Obj.TEMP} run return 0")
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)

		// Fire. The latch tells the top of the next tick to put it out again.
		tag(self()) { add(ON) }
		execute {
			at(self())
			run { outputShell(on = true) }
		}
	}

	/** Goggles readout; also the single source of the block's display name (see ra:tools/block_name). */
	private fun Function.goggles() {
		gogglesHeader(block, nameY = 0.7)
		propLine("cooldown", "Cooldown: ", "aqua", y = 0.45, suffix = "t")
	}
}
