package ra.lib.redstone

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.macroCall
import ra.core.raw
import ra.lib.fake
import ra.lib.lib
import ra.lib.redstone.Rules.AT

/**
 * The per-neighbour readers -- `side` (the level), `any_side` (yes/no) and `has_input`
 * (presence) -- and the whole-block questions built on them: `any` and `count_inputs`.
 */
internal object SideReaders {
	val anySide = lib("redstone/any_side")
	val hasInput = lib("redstone/has_input")

	private const val R = "#rs ${Obj.TEMP}"

	/** The argument block every rule function takes, as macro text. */
	private const val DUST_ARGS = "{dx:\$(dx),dy:\$(dy),dz:\$(dz),back:\"\$(back)\"}"
	private const val TORCH_ARGS = "{dx:\$(dx),dy:\$(dy),dz:\$(dz),side:\"\$(side)\"}"

	fun Ra.registerSideReaders() {
		pack.define(RaLib.Redstone.side) { side() }
		pack.define(anySide) { anySide() }
		pack.define(hasInput) { hasInput() }

		// `any`: is this block powered from any side at all? Returns 1 or 0.
		// Context: as the block's marker, at the block position.
		//
		// The cheap question, for the many blocks that only ever ask "am I on?". It reads no
		// levels, writes no scores and sets no tags, and it stops at the first side that answers yes.
		//
		// BEFORE YOU CALL THIS
		// If the block underneath is one vanilla already tracks, do not call this at all -- read the
		// block state and pay nothing. A dispenser or dropper carries `triggered`, a note block and
		// a door carry `powered`, a redstone lamp carries `lit`. The Block Breaker already does
		// exactly this with `dispenser[triggered=true]`. This function is for the blocks with no
		// such state -- quartz, purpur, bookshelf, terracotta -- where the neighbours have to be
		// asked directly.
		//
		// `execute if function` cannot carry macro arguments, so each side's answer is captured
		// with `store result` and tested straight after. Still early-exits: the `return 1` leaves
		// this function before the remaining sides are asked. #rs.any is a separate holder from the
		// #rs that any_side works in, so reading the answer cannot disturb how it was reached.
		pack.define(RaLib.Redstone.any) {
			val any = fake("#rs.any")
			for (s in Side.entries) {
				execute {
					storeResult { score(any, Obj.TEMP) }
					run { function(anySide, s.args()) }
				}
				execute {
					ifCondition { score(any, Obj.TEMP) greaterThanOrEqualTo 1 }
					run { returnValue(1) }
				}
			}
			returnValue(0)
		}

		// `count_inputs`: count how many sides carry a redstone component, powered or not.
		// As the marker, at the block. Output: @s ra.rs_inputs (0-6).
		//
		// Used by the AND and NAND gates, which have to know how many inputs exist before they can
		// say whether all of them are on.
		//
		// This used to be a hand-copied mirror of the old detect: every source test written out
		// again for all six directions, with a comment warning that the two had to be kept in step
		// by hand. They never quite were -- detect grew the ra.redstone.ignore_blocks opt-out and
		// this file grew it too, but neither ever learned about pressure plates. Both now share
		// ra_lib:redstone/has_input, so a source added to the block tags is counted here without
		// anyone remembering to.
		pack.define(RaLib.Redstone.countInputs) {
			val input = fake("#rs.in")
			scoreboard.players.set(self(), Obj.RS_INPUTS, 0)
			for (s in Side.entries) {
				execute {
					storeResult { score(input, Obj.TEMP) }
					run { function(hasInput, s.args()) }
				}
				scoreboard.players.operation(self(), Obj.RS_INPUTS, Operation.ADD, input, Obj.TEMP)
			}
			returnRun { scoreboard.players.get(self(), Obj.RS_INPUTS) }
		}
	}

	/**
	 * `side {dx,dy,dz,side,back,torch,dust}`: the power one neighbour delivers into this block,
	 * 0-16. As the block's marker, at the block position. Returns the level, and leaves it in #rs.
	 *
	 * Prefer the ra_lib:redstone/local/... wrappers, which name the side the way the player sees
	 * it and work out the world direction from the block's own facing. Call this directly only
	 * when you really do mean a compass direction. The arguments are described on [Side].
	 *
	 * WHY dust AND torch ARE BOTH NAMED BY THE CALLER
	 * This function used to substitute $(back) straight into a redstone_wire block state on every
	 * side. redstone_wire has north, south, east and west connection states and nothing else, so
	 * the two vertical sides asked it for `up` and `down`. A macro line is parsed after
	 * substitution and before the function runs, so that one unparseable line stopped the whole
	 * function from executing -- and the up and down sides therefore read 0 for EVERY source, not
	 * just for dust. A lever sitting directly on a block never turned it on. Naming the rule per
	 * side is what stops a vertical side being handed a horizontal test again.
	 *
	 * LEVELS
	 *   0       nothing
	 *   1..15   ordinary power, the number the source is actually carrying
	 *   16      superpower -- a repeater, comparator or observer pointed into this block. Vanilla calls this
	 *           strong power at level 15; the pack numbers it 16 so a block can tell "a full dust
	 *           line reached me" from "a repeater is driving me", which is the distinction that
	 *           matters when deciding whether to pass a signal on.
	 *
	 * ORDER
	 * Sources are written weakest-first so a plain overwrite settles on the strongest, with no
	 * maximum to compute. Anything reading this must not reorder the block.
	 */
	private fun io.github.ayfri.kore.functions.Function.side() {
		scoreboard.players.set(fake("#rs"), Obj.TEMP, 0)

		// Dust rules differ by side, so the side names which one applies -- exactly as the torch
		// rules below do. Horizontal neighbours are gated on the connection state facing us; dust
		// above powers us whatever shape it is in; dust below never does.
		raw("\$function ${Rules.dust.asId()}/\$(dust) $DUST_ARGS")

		// Analog sources that power every neighbour regardless of orientation: the two weighted
		// pressure plates and the daylight detector. No connection test needed.
		raw("\$execute if block $AT ${RedstoneTags.ANALOG_OMNI} run ${macroCall(Rules.analog, "{dx:\$(dx),dy:\$(dy),dz:\$(dz)}")}")

		// Everything with a `powered` flag that drives all its neighbours at full strength when set:
		// levers, every button, the non-weighted pressure plates, tripwire hooks, lightning rods.
		// One tag, one test -- the old library spent twelve commands a side on levers and buttons
		// alone and still never noticed a pressure plate.
		raw("\$execute if block $AT ${RedstoneTags.BINARY_SOURCES}[powered=true] run scoreboard players set $R 15")

		// A redstone block is a source that is always on. Blocks that sit inside redstone machinery
		// opt out with ra.redstone.ignore_blocks, because the pack itself uses redstone blocks as
		// gate output and a gate must not read its own answer back.
		raw("\$execute unless entity @s[tag=$IGNORE_BLOCKS] if block $AT minecraft:redstone_block run scoreboard players set $R 15")

		// Torch rules differ by side, so the side names which one applies.
		raw("\$function ${Rules.torch.asId()}/\$(torch) $TORCH_ARGS")

		// Sources that drive one direction only, and strongly: a repeater, a comparator, an
		// observer pulsing. None of the three names its output with `facing`: on a repeater and a
		// comparator `facing` points from the output towards the input, and on an observer it is
		// the side being watched, with the output on the far side. Either way they output towards
		// the OPPOSITE of `facing`, so the one driving us is the one whose `facing` is `$(side)` --
		// the direction from us towards it. A repeater facing `$(back)` points AWAY from us and
		// must not count. `powered` says whether it is on. One tag test where this used to be three.
		//
		// A comparator's real output level lives in its block entity (OutputSignal), not its block
		// state; it is read as superpower like the other two, which is what an on/off input needs.
		//
		// This is the 16 the level scale reserves for superpower. Vanilla calls it strong power at
		// 15; the pack numbers it above a full dust line so a block can tell "something is
		// deliberately driving me" from "a signal happened to arrive".
		raw("\$execute if block $AT ${RedstoneTags.DIRECTIONAL_SOURCES}[facing=\$(side),powered=true] run scoreboard players set $R 16")

		returnRun { scoreboard.players.get(fake("#rs"), Obj.TEMP) }
	}

	/**
	 * `any_side {...}`: is this one neighbour powering us at all? Returns 1 or 0.
	 *
	 * Same sources as ra_lib:redstone/side, but every test bails out the moment it hits, and none
	 * of them resolves a level. The expensive part of reading redstone is working out *how much*
	 * -- fifteen state comparisons for a dust or a weighted plate -- and a caller that only wants
	 * to know whether it is on should never pay for that. Testing dust against power=0 answers
	 * "carrying anything" in one comparison instead of fifteen.
	 *
	 * Ordered cheapest and commonest first, so the usual answer costs one or two tests. Cost is
	 * only fully paid on a side with nothing on it.
	 */
	private fun io.github.ayfri.kore.functions.Function.anySide() {
		scoreboard.players.set(fake("#rs"), Obj.TEMP, 0)

		raw("\$execute if block $AT ${RedstoneTags.BINARY_SOURCES}[powered=true] run return 1")
		raw("\$execute unless entity @s[tag=$IGNORE_BLOCKS] if block $AT minecraft:redstone_block run return 1")

		// Dust rules are per-side and already written down once; reuse them. `execute if function`
		// cannot carry macro arguments, so the answer comes back as a score.
		raw("\$execute store result score #rs.dust ${Obj.TEMP} run function ${Rules.dustAny.asId()}/\$(dust) $DUST_ARGS")
		dustAnswer()

		// Pointing at us, i.e. `facing` is `$(side)` -- see ra_lib:redstone/side.
		raw("\$execute if block $AT ${RedstoneTags.DIRECTIONAL_SOURCES}[facing=\$(side),powered=true] run return 1")

		raw("\$execute if block $AT ${RedstoneTags.ANALOG_OMNI} unless block $AT ${RedstoneTags.ANALOG_OMNI}[power=0] run return 1")

		// Torch rules are per-side and already written down once; reuse them rather than restate
		// them, and read the level they leave behind as a yes or no.
		raw("\$function ${Rules.torch.asId()}/\$(torch) $TORCH_ARGS")
		execute {
			ifCondition { score(fake("#rs"), Obj.TEMP) greaterThanOrEqualTo 1 }
			run { returnValue(1) }
		}

		returnValue(0)
	}

	/**
	 * `has_input {...}`: is there a redstone component on this side that could power us, whether
	 * or not it currently is? Returns 1 or 0.
	 *
	 * The presence twin of any_side. Same sources, same orientation rules, same opt-out -- a
	 * component only counts where any_side would be able to read power from it -- but with every
	 * powered/lit test dropped. That is the whole point: it lets a caller tell "an input is here
	 * and it is off" apart from "there is no input on this side", which an AND gate needs and a
	 * power reading cannot give it.
	 */
	private fun io.github.ayfri.kore.functions.Function.hasInput() {
		raw("\$execute if block $AT ${RedstoneTags.BINARY_SOURCES} run return 1")
		raw("\$execute unless entity @s[tag=$IGNORE_BLOCKS] if block $AT minecraft:redstone_block run return 1")

		raw("\$execute store result score #rs.dust ${Obj.TEMP} run function ${Rules.dustPresent.asId()}/\$(dust) $DUST_ARGS")
		dustAnswer()

		// Only one pointing at us is an input; one pointing away is just a neighbour.
		raw("\$execute if block $AT ${RedstoneTags.DIRECTIONAL_SOURCES}[facing=\$(side)] run return 1")

		raw("\$execute if block $AT ${RedstoneTags.ANALOG_OMNI} run return 1")

		raw("\$return run function ${Rules.torchPresent.asId()}/\$(torch) $TORCH_ARGS")
	}

	private fun io.github.ayfri.kore.functions.Function.dustAnswer() = execute {
		ifCondition { score(fake("#rs.dust"), Obj.TEMP) greaterThanOrEqualTo 1 }
		run { returnValue(1) }
	}

	/** Marker tag: this block does not read redstone blocks as a source. */
	const val IGNORE_BLOCKS = "ra.redstone.ignore_blocks"
}
