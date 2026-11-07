package ra.modules.wires.blocks.industrial_light

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Goggles
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.line
import ra.modules.wires.electric.EuApi

object IndustrialLight : PackPart {
	val cast = ref(Ns.WIRES, "blocks/industrial_light/cast")
	val dark = ref(Ns.WIRES, "blocks/industrial_light/dark")
	val goggles = ref(Ns.WIRES, "blocks/industrial_light/goggles")
	val off = ref(Ns.WIRES, "blocks/industrial_light/off")
	val space = ref(Ns.WIRES, "blocks/industrial_light/space")
	val step = ref(Ns.WIRES, "blocks/industrial_light/step")
	val tick = ref(Ns.WIRES, "blocks/industrial_light/tick")

	override fun Ra.register() {
		pack.define(IndustrialLight.cast) {
			// Internal: walk the beam out from this block and set or clear each space.
			// Context: as the marker, at its block.
			//
			// ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. The marker is given
			// one at placement (see ra_lib:placement/place), so this never has to ask a
			// player where they were looking.
			//
			// The mode travels as a SCORE and arrives as a NUMBER, not as the word "on". It
			// used to be `beam.mode`, tested in `space` with
			// `if data storage ra:wires beam{mode:"on"}`, on lines that began with `$` while
			// containing no macro placeholder at all -- two constructs whose behaviour I
			// could not confirm from outside the game, sitting in the exact path that was
			// doing nothing. A number substituted into a scoreboard set, compared with
			// `matches`, has no such doubt attached to it.

			data(Storage.WIRES) { remove("beam") }
			scoreboard.players.set(literal("#il.n"), "ra.wires.tmp", 0)

			raw("\$scoreboard players set #il.mode ra.wires.tmp2 \$(mode)")

			raw("execute if score @s ra.facing matches 0 run data modify storage ra:wires beam set value {dx:0,dy:-1,dz:0}")
			raw("execute if score @s ra.facing matches 1 run data modify storage ra:wires beam set value {dx:0,dy:1,dz:0}")
			raw("execute if score @s ra.facing matches 2 run data modify storage ra:wires beam set value {dx:0,dy:0,dz:-1}")
			raw("execute if score @s ra.facing matches 3 run data modify storage ra:wires beam set value {dx:0,dy:0,dz:1}")
			raw("execute if score @s ra.facing matches 4 run data modify storage ra:wires beam set value {dx:-1,dy:0,dz:0}")
			raw("execute if score @s ra.facing matches 5 run data modify storage ra:wires beam set value {dx:1,dy:0,dz:0}")

			// A marker with no facing score at all would otherwise leave dx/dy/dz undefined
			// and the macro in `step` would fail on a missing variable.
			raw("execute unless data storage ra:wires beam.dx run data modify storage ra:wires beam set value {dx:0,dy:0,dz:1}")

			function(IndustrialLight.step, Storage.WIRES, "beam")
		}

		pack.define(IndustrialLight.dark) {
			// Put the beam out and say which of the two conditions failed.
			// Context: as the marker, at its block.
			//
			// `off` reported a bare "Dark", which is true and useless: a light that is not lit
			// is either unpowered or unpaid, and those are fixed at opposite ends of the
			// build. The goggles now name the one that is missing.
			//
			// WHY THE CLEAR IS CONDITIONAL
			// This used to walk and clear the whole ten-block beam on every tick the light
			// was not lit -- about sixty commands per light per tick, for ever, for lights
			// that are simply switched off. A switched-off light does not need clearing
			// repeatedly; it needs clearing once, on the tick it goes out.
			//
			// The periodic sweep is what the unconditional version was really for: a beam
			// orphaned by a chunk unload has no marker left to notice the transition. Once
			// every ten seconds is often enough for something that should never happen, and
			// it costs a two-hundredth of what running it every tick did.

			raw("\$data modify entity @s data.status.beam set value \"\$(why)\"")
			raw("data modify entity @s data.status.active set value 0b")

			executeRun("if entity @s[tag=ra.wires.light_lit]", IndustrialLight.cast, "{mode:0}")
			executeRun("if entity @s[tag=!ra.wires.light_lit] if score #il.sweep ra.wires.tmp2 matches 0", IndustrialLight.cast, "{mode:0}")
			tag(self()) { remove("ra.wires.light_lit") }
		}

		pack.define(IndustrialLight.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.

			gogglesHeader("Industrial Light", nameY = 1.0)

			dataLine("beam", "Beam: ", "yellow", y = 0.8)
			dataLine("available_eu", "EU: ", "aqua", y = 0.55, suffix = " EU")
			dataLine("grid", "", "gray", y = 0.3)
		}

		pack.define(IndustrialLight.off) {
			// kept: beet contract (nothing in the pack calls it).
			// Put the beam out with no explanation. Context: as the marker, at its block.
			//
			// Used for the one case that is not a failure: the light is switched off in its
			// own properties, so naming a missing condition would be misleading.

			function(IndustrialLight.dark, nbt { this["why"] = "Disabled" })
		}

		pack.define(IndustrialLight.space) {
			// Internal: one space of the beam, then carry on down it.
			// Context: at the space.
			//
			// Lighting stops at the first thing that is not air: a beam should not shine
			// through a wall. Unlighting does not stop, because the wall may have been built
			// after the beam was lit and the lights beyond it still have to come back out.
			//
			// `minecraft:air` and `minecraft:cave_air` by name rather than the `#minecraft:air`
			// tag. Underground, every empty space is cave_air, so a tag that covers both is
			// the tidier test -- but the tag is one more thing that has to be right in a chain
			// that was silently doing nothing, and naming the two blocks costs one command.
			//
			// Removal is still pinned to level=15, the exact state this places. Nothing a
			// player built, including a light block of any other level, can be removed here.

			scoreboard.players.set(literal("#il.ok"), "ra.wires.tmp2", 0)
			raw("execute if block ~ ~ ~ minecraft:air run scoreboard players set #il.ok ra.wires.tmp2 1")
			raw("execute if block ~ ~ ~ minecraft:cave_air run scoreboard players set #il.ok ra.wires.tmp2 1")
			raw("execute if block ~ ~ ~ minecraft:light[level=15] run scoreboard players set #il.ok ra.wires.tmp2 1")

			raw("execute if score #il.mode ra.wires.tmp2 matches 1 if score #il.ok ra.wires.tmp2 matches 1 run setblock ~ ~ ~ minecraft:light[level=15]")
			raw("execute if score #il.mode ra.wires.tmp2 matches 1 if score #il.ok ra.wires.tmp2 matches 0 run return 0")

			raw("execute if score #il.mode ra.wires.tmp2 matches 0 if block ~ ~ ~ minecraft:light[level=15] run setblock ~ ~ ~ minecraft:air")

			function(IndustrialLight.step, Storage.WIRES, "beam")
		}

		pack.define(IndustrialLight.step) {
			// Internal: move one space along the beam. Context: at the last space handled.
			//
			// Recursion rather than ten written-out offsets, because each step has to be
			// positioned relative to the one before it -- a macro cannot multiply its own
			// argument by a counter.

			scoreboard.players.add(literal("#il.n"), "ra.wires.tmp", 1)
			raw("execute if score #il.n ra.wires.tmp matches 11.. run return 0")

			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) run function ${IndustrialLight.space.asId()}")
		}

		pack.define(IndustrialLight.tick) {
			// Project a beam of light while powered and paid for.
			// Context: as the light's marker, at its block.
			//
			// Two conditions, both required: a redstone signal to say you want it on, and EU
			// on the grid to pay for it. Losing either puts the beam out, which is the point
			// of it being an electric block rather than a lamp.
			//
			// The beam is minecraft:light, the invisible block the game uses for its own
			// lighting fixes. It is not a light source you can craft, so anything found in
			// the beam's path is either ours or something a player placed deliberately with a
			// command -- and either way the only block this will ever remove is a light.
			//
			// WHY THE DRAW IS READ THROUGH util/property
			// It used to be `store result … run data get entity @s data.properties.eu_use`.
			// A failed read of a missing path does not error, it stores ZERO -- so a light
			// whose eu_use had never been written asked the grid for nothing, got nothing
			// back, and read "took 0" as "the grid cannot pay". It sat dark on a full
			// battery bank and no amount of EU would ever change that, because the amount
			// was never the problem. util/property keeps the default when the read fails.

			line("execute unless function", RaLib.Redstone.any, "run return run function", IndustrialLight.dark, """{why:"No redstone"}""")

			// Pay for this tick before lighting it. A grid that cannot cover the draw leaves
			// the beam dark rather than running it for free.
			function(RaLib.Util.property, nbt { this["name"] = "eu_use"; this["default"] = 10; this["min"] = 0 })
			scoreboard.players.operation(literal("#il.use"), "ra.wires.tmp", Operation.SET, literal("#prop"), "ra.temp")

			// A light configured to draw nothing is free, and must not be made to prove it
			// can pay -- taking 0 always returns 0, which is the refusal value.
			scoreboard.players.set(literal("#il.got"), "ra.wires.tmp", 1)
			raw("execute if score #il.use ra.wires.tmp matches 1.. store result storage ra:wires eu.amount int 1 run scoreboard players get #il.use ra.wires.tmp")
			executeRun("if score #il.use ra.wires.tmp matches 1.. store result score #il.got ra.wires.tmp", EuApi.takeEu, "with storage ra:wires eu")
			executeReturnRun("if score #il.got ra.wires.tmp matches ..0", IndustrialLight.dark, """{why:"No EU"}""")

			raw("data modify entity @s data.status.active set value 1b")
			raw("data modify entity @s data.status.beam set value \"Lit\"")
			tag(self()) { add("ra.wires.light_lit") }
			function(IndustrialLight.cast, nbt { this["mode"] = 1 })
		}
	}
}
