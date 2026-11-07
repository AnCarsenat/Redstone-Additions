package ra.lib.redstone

import io.github.ayfri.kore.commands.returnValue
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.lib
import ra.core.macroCall

/**
 * The per-side dust and torch rules, in their three flavours: the level (`dust/`, `torch/`), the
 * cheap yes/no (`dust_any/`) and presence regardless of power (`dust_present/`, `torch_present/`).
 *
 * The side names which rule applies (`dust:"side"`, `torch:"below"`...), and the readers dispatch
 * on that name, so a vertical side can never be handed a horizontal test again.
 */
internal object Rules {
	/** The neighbour position every rule tests, as macro text. */
	const val AT = "~\$(dx) ~\$(dy) ~\$(dz)"

	/** Directory prefixes the readers append `/$(dust)` / `/$(torch)` to. */
	val dust = lib("redstone/dust")
	val dustAny = lib("redstone/dust_any")
	val dustPresent = lib("redstone/dust_present")
	val torch = lib("redstone/torch")
	val torchPresent = lib("redstone/torch_present")

	val analog = lib("redstone/analog")

	private const val R = "#rs ${Obj.TEMP}"
	private const val ANALOG_ARGS = "{dx:\$(dx),dy:\$(dy),dz:\$(dz)}"

	fun Ra.registerRules() {
		// `analog {dx,dy,dz}`: read the exact 1-15 `power` state of the analog source at the offset
		// and leave it in #rs ra.temp.
		//
		// Only called once a caller has confirmed a source is actually there, so the fifteen tests
		// below are paid on the rare side that has one rather than on every side of every block.
		// #ra_lib:redstone/analog_sources holds every block whose `power` state is the level it
		// hands to its neighbours: redstone dust, the two weighted pressure plates and the daylight
		// detector. Anything else with a `power` state can join the tag and needs no code change here.
		pack.define(analog) {
			for (level in 1..15) raw("\$execute if block $AT ${RedstoneTags.ANALOG_SOURCES}[power=$level] run scoreboard players set $R $level")
		}

		// dust/side {dx,dy,dz,back}: redstone dust on a horizontal neighbour. Leaves the level in #rs.
		//
		// Dust only counts when it is pointing at us: a line running past the side does not power
		// what it passes. `$(back)` is the connection state on the neighbour's side that faces us,
		// and `up` is the same connection climbing a block.
		//
		// Only a horizontal neighbour has a connection state naming us. Dust above and below is a
		// different rule entirely -- see dust/above and dust/none.
		pack.define(lib("redstone/dust/side")) {
			for (conn in listOf("side", "up")) raw("\$execute if block $AT minecraft:redstone_wire[\$(back)=$conn] run ${macroCall(analog, ANALOG_ARGS)}")
		}

		// dust/above {dx,dy,dz}: redstone dust on the neighbour directly above. Leaves the level in #rs.
		//
		// Dust powers the block it is lying on, whichever way the line happens to run -- that is how
		// a lamp under a dust line lights. So there is no connection test here, and there cannot be
		// one: redstone_wire has only north, south, east and west connection states, and no `down`.
		// Asking it for one is what broke this side for every source, not just dust; see dust/none.
		pack.define(lib("redstone/dust/above")) {
			raw("\$execute if block $AT minecraft:redstone_wire run ${macroCall(analog, ANALOG_ARGS)}")
		}

		// dust/none: the side where dust is never an input -- the one below us.
		//
		// Dust powers the block underneath it, not the one above, so a line on the floor below does
		// not reach us. Dust that climbs the side of our block is on a horizontal neighbour with its
		// connection state set to `up`, and dust/side already reads that.
		//
		// Deliberately empty, so that this rule is written down rather than being an unexplained
		// missing case. It exists for the same reason torch/none does.
		pack.define(lib("redstone/dust/none")) {}

		// dust_any/side: is the dust on this horizontal neighbour carrying anything? Returns 1 or 0.
		// The cheap twin of dust/side -- testing against power=0 answers in one comparison where
		// resolving the level costs fifteen.
		pack.define(lib("redstone/dust_any/side")) {
			for (conn in listOf("side", "up")) raw("\$execute if block $AT minecraft:redstone_wire[\$(back)=$conn] unless block $AT minecraft:redstone_wire[power=0] run return 1")
			returnValue(0)
		}

		// dust_any/above: is the dust directly above carrying anything? The cheap twin of dust/above
		// -- no connection test, because dust powers whatever it lies on.
		pack.define(lib("redstone/dust_any/above")) {
			raw("\$execute if block $AT minecraft:redstone_wire unless block $AT minecraft:redstone_wire[power=0] run return 1")
			returnValue(0)
		}

		// dust_any/none: the side below, where dust is never an input. Mirrors dust/none.
		// Deliberately returns 0.
		pack.define(lib("redstone/dust_any/none")) { returnValue(0) }

		// dust_present/side: is there dust on this horizontal neighbour pointing at us, powered or
		// not? Mirrors dust/side minus the level.
		pack.define(lib("redstone/dust_present/side")) {
			for (conn in listOf("side", "up")) raw("\$execute if block $AT minecraft:redstone_wire[\$(back)=$conn] run return 1")
			returnValue(0)
		}

		// dust_present/above: is there dust directly above, powered or not? Mirrors dust/above minus the level.
		pack.define(lib("redstone/dust_present/above")) {
			raw("\$execute if block $AT minecraft:redstone_wire run return 1")
			returnValue(0)
		}

		// dust_present/none: deliberately returns 0. Mirrors dust/none.
		pack.define(lib("redstone/dust_present/none")) { returnValue(0) }

		// torch/side {dx,dy,dz,side}: redstone torches on a horizontal neighbour.
		//
		// A standing torch is deliberately not read here. A redstone torch strongly powers the block
		// ABOVE it and nothing else -- it does not power the blocks beside it, so a torch standing
		// next to this one is not an input.
		//
		// A wall torch does power the block beside it, with one exception: never the block it is
		// mounted on. A wall torch points away from whatever it is attached to, so the one attached
		// to US is the one facing $(side) -- the direction from us towards that neighbour. Excluding
		// that single facing is cheaper and clearer than listing the three that do count.
		pack.define(lib("redstone/torch/side")) {
			raw("\$execute if block $AT minecraft:redstone_wall_torch[lit=true] unless block $AT minecraft:redstone_wall_torch[facing=\$(side)] run scoreboard players set $R 15")
		}

		// torch/below: redstone torches on the neighbour underneath.
		//
		// This is the one side where a torch counts: a redstone torch strongly powers the block
		// directly above it, and that block is us. Both shapes do it -- a torch standing on the floor
		// below and a wall torch mounted below -- and for a wall torch the facing does not matter,
		// because it is powering upwards, not sideways.
		pack.define(lib("redstone/torch/below")) {
			for (t in listOf("redstone_torch", "redstone_wall_torch")) raw("\$execute if block $AT minecraft:$t[lit=true] run scoreboard players set $R 15")
		}

		// torch/none: the side where no torch can ever be an input -- the one above us.
		//
		// A torch up there is either standing ON this block, in which case it is attached to us and
		// a torch never powers what it is mounted on, or it is a wall torch powering the block above
		// itself, which is not us either way.
		//
		// Deliberately empty. It exists so ra_lib:redstone/side can dispatch on a name instead of
		// branching, and so that this rule is written down somewhere rather than being an
		// unexplained missing case. The old library powered a block from a torch standing on top of
		// it, which is not how vanilla behaves.
		pack.define(lib("redstone/torch/none")) {}

		// torch_present/side: is there a wall torch on this horizontal neighbour that could power
		// us? Presence only -- lit or not. Mirrors torch/side minus the lit test, so the two must be
		// kept in step.
		pack.define(lib("redstone/torch_present/side")) {
			raw("\$execute if block $AT minecraft:redstone_wall_torch unless block $AT minecraft:redstone_wall_torch[facing=\$(side)] run return 1")
			returnValue(0)
		}

		// torch_present/below: is there a torch underneath that could power us? Presence only.
		// Mirrors torch/below minus the lit test.
		pack.define(lib("redstone/torch_present/below")) {
			for (t in listOf("redstone_torch", "redstone_wall_torch")) raw("\$execute if block $AT minecraft:$t run return 1")
			returnValue(0)
		}

		// torch_present/none: the side above, where no torch is ever an input. Mirrors torch/none.
		// Deliberately returns 0.
		pack.define(lib("redstone/torch_present/none")) { returnValue(0) }
	}
}
