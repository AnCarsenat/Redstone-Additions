package ra.lib.redstone

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.lib.lib

/** `detect`, `detect_switch`, `detect_local` and the look-space `facing/...` mappings. */
internal object Detect {
	private const val STRONG = "ra.powered.strong"

	fun Ra.registerDetect() {
		pack.define(RaLib.Redstone.detect) { detect() }

		// `detect_switch`: maintain ra.powered for a block that only cares whether it is on.
		// As the marker, at the block. Returns 1 or 0.
		//
		// For the blocks that use redstone as a switch rather than as a number -- the Chunk
		// Loader, the Boxer and Unboxer, both tag sensors, the Message Block, the wireless Emitter.
		// They were all calling ra_lib:redstone/detect, which resolves the exact level on all six
		// sides and then had its answer thrown away by a `matches 1..` test.
		//
		// This stops at the first live side instead of reading all six, so a powered block usually
		// answers in a fraction of the work. An unpowered one still has to look everywhere before
		// it can say no -- that is the floor for asking the question at all, and there is no way
		// under it that does not involve the block itself carrying a vanilla state.
		//
		// DOES NOT WRITE ra.power. Callers must test the ra.powered tag, not the score, or they will
		// read whatever the last full detect left behind.
		pack.define(RaLib.Redstone.detectSwitch) {
			tag(self()) { remove(Tag.POWERED) }
			tag(self()) { remove(STRONG) }

			execute {
				ifCondition { function(RaLib.Redstone.any) }
				run { tag(self()) { add(Tag.POWERED) } }
			}

			execute {
				ifCondition { entity(self { tag = Tag.POWERED }) }
				run { returnValue(1) }
			}
			returnValue(0)
		}

		// facing/<f>: map world-space power scores onto look-space ones for a marker with that
		// facing.
		//
		// A block placed pointing straight up or down stores no yaw, so left, right, local_up and
		// local_down cannot be derived -- they are a convention. The one chosen here is that up and
		// down are mirror images of each other, which is what rotating the block through the
		// horizontal would do: facing up has left=east, facing down has left=west. They used to
		// agree instead, so flipping a block over left its left and right hands unchanged, which no
		// real rotation does. Front and back are the only two that are facts on a vertical block.
		for ((facing, map) in lookSpace) pack.define(facingFn(facing)) {
			for ((local, world) in map) scoreboard.players.operation(self(), local.objective, Operation.SET, self(), world.objective)
		}

		pack.define(RaLib.Redstone.detectLocal) { detectLocal() }
	}

	private fun facingFn(facing: Side) = lib("redstone/facing/${facing.id}")

	/**
	 * `detect`: read the redstone power reaching this block. As the marker, at the block.
	 *
	 * Output:
	 *   ra.power                                 strongest side, 0-16
	 *   ra.power.north/south/east/west/up/down   per side, 0-16
	 *   ra.powered                               set when ra.power is 1 or more
	 *   ra.powered.strong                        set when ra.power is 16
	 *
	 * Returns ra.power, so a caller can branch on it without reading the score back.
	 *
	 * WHAT CHANGED, AND WHY IT COSTS LESS
	 * This used to be seven hand-written source scanners, each repeating the same six directions
	 * with the offsets typed out by hand -- 505 lines whose dust section alone was fifteen
	 * near-identical comparisons times four. Every side is now one call to ra_lib:redstone/side,
	 * which knows every source once. Adding a source is a block-tag edit; it can no longer be
	 * added to one direction and forgotten in another, which is how pressure plates came to be
	 * missing from all six.
	 *
	 * The rest of the saving is in what is no longer computed by default. The look-space scores
	 * and the twelve direction tags had, at the time of writing, exactly zero readers anywhere in
	 * the pack, yet every call cleared twenty-two tags and wrote six extra scores to keep them
	 * current. They now live in ra_lib:redstone/detect_local, which costs nothing unless a block
	 * asks for it.
	 *
	 * The per-source tags (ra.powered.dust, .lever, .torch and the rest) are gone. Nothing read
	 * them either, and reproducing them would mean the per-side reader reporting which source won
	 * rather than just how much -- a cost paid on every call for information nobody wanted.
	 *
	 * CHEAPER STILL
	 * A block that only needs "am I on?" should call ra_lib:redstone/any, and one that cares about
	 * a single face should call ra_lib:redstone/local/front and friends. A block whose vanilla base
	 * already carries the answer -- dispenser `triggered`, note block `powered`, redstone lamp
	 * `lit` -- should read the block state and call nothing at all.
	 */
	private fun Function.detect() {
		for (s in Side.entries) execute {
			storeResult { score(self(), s.objective) }
			run { function(RaLib.Redstone.side, s.args()) }
		}

		// Aggregate: the strongest side wins.
		scoreboard.players.operation(self(), Obj.POWER, Operation.SET, self(), Side.NORTH.objective)
		for (s in Side.entries.drop(1)) execute {
			ifCondition { score(self(), s.objective) greaterThan score(self(), Obj.POWER) }
			run { scoreboard.players.operation(self(), Obj.POWER, Operation.SET, self(), s.objective) }
		}

		// Both tags are removed unconditionally first: a block that was powered last tick and is not
		// now has to lose them, and these two are the only ones this function is responsible for.
		tag(self()) { remove(Tag.POWERED) }
		tag(self()) { remove(STRONG) }
		execute {
			ifCondition { score(self(), Obj.POWER) greaterThanOrEqualTo 1 }
			run { tag(self()) { add(Tag.POWERED) } }
		}
		execute {
			ifCondition { score(self(), Obj.POWER) equalTo 16 }
			run { tag(self()) { add(STRONG) } }
		}

		returnRun { scoreboard.players.get(self(), Obj.POWER) }
	}

	/**
	 * `detect_local`: ra_lib:redstone/detect, plus the look-space view of the same reading.
	 * As the marker, at the block. Returns ra.power.
	 *
	 * Adds on top of detect:
	 *   ra.power.front/back/left/right/local_up/local_down
	 *   ra.powered.{north,south,east,west,up,down}
	 *   ra.powered.{front,back,left,right,local_up,local_down}
	 *
	 * Split out from detect because nothing in the pack reads any of it, and making every
	 * redstone-reading block pay for twenty-two tag removals and six extra scores to maintain an
	 * unread view was most of what detect cost. It stays available because it is documented as
	 * part of the library's contract and an addon may well want it.
	 *
	 * If you are writing new code, prefer ra_lib:redstone/local/front and its five siblings: they
	 * read one named side for about a tenth of what this costs, and they do not need the whole
	 * reading computed first.
	 */
	private fun Function.detectLocal() {
		function(RaLib.Redstone.clear)
		function(RaLib.Redstone.detect)

		// Look-space mapping from the marker's own facing.
		facings.forEachIndexed { i, facing ->
			execute {
				ifCondition { score(self(), Obj.FACING) equalTo i }
				run { function(facingFn(facing)) }
			}
		}

		// Direction tags, only worth testing when something is powered at all.
		execute {
			ifCondition { score(self(), Obj.POWER) greaterThanOrEqualTo 1 }
			run { function(RedstoneTags.tags) }
		}

		returnRun { scoreboard.players.get(self(), Obj.POWER) }
	}
}
