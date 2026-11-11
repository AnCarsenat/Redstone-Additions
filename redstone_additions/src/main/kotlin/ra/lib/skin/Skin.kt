package ra.lib.skin

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.skin.Spawn.registerSpawn
import ra.core.macroCall

/**
 * `ra_lib:skin/...`: draw one block's look over another block's mechanics.
 *
 * WHY THIS EXISTS
 * Some vanilla blocks carry behaviour you cannot switch off. A dispenser fires its own inventory on
 * any rising redstone edge; a dropper does the same. A custom block that stores items in itself
 * and sits in a redstone build will therefore eject them, and no amount of datapack logic can
 * intercept it.
 *
 * The fix is to place a block whose behaviour you DO want -- a barrel gives the same inventory and
 * GUI with no dispense -- and put the appearance back with a block_display. Mechanics and looks
 * stop being the same decision.
 */
object Skin {
	private val facings = listOf("north", "south", "east", "west", "up", "down")

	fun Ra.registerSkin() {
		// `apply {real, skin, id}`: draw `skin` over the `real` block at the current position.
		// Context: at the block position.
		//
		// The facing is read back off the real block rather than from stored state, so a block
		// rotated by any means still gets a skin pointing the same way, and a skin that drifts out
		// of sync repairs itself the next time this runs.
		//
		// For blocks with no `facing` property, use ra_lib:skin/apply_static.
		pack.define(RaLib.Skin.apply) {
			killSkins()
			raw("\$execute unless block ~ ~ ~ \$(real) run return 0")
			sampleLight()
			for (f in facings) raw(
				"\$execute if block ~ ~ ~ \$(real)[facing=$f] run " +
					macroCall(Spawn.spawnWithLight, "{skin:\"\$(skin)\",id:\"\$(id)\",facing:\"$f\"}")
			)
			returnValue(1)
		}

		// `apply_static {real, skin, id}`: same as apply, for a skin block that has no `facing` property.
		pack.define(RaLib.Skin.applyStatic) {
			killSkins()
			raw("\$execute unless block ~ ~ ~ \$(real) run return 0")
			sampleLight()
			raw("\$" + macroCall(Spawn.spawnStaticWithLight, "{skin:\"\$(skin)\",id:\"\$(id)\"}"))
			returnValue(1)
		}

		// `apply_facing {real, skin, id, facing}`: the skin always turned to `facing`, over a real
		// block with no `facing` of its own (the Item Pipe Splitter: a dispenser facing up drawn over
		// a block that is not a container).
		pack.define(RaLib.Skin.applyFacing) {
			killSkins()
			raw("\$execute unless block ~ ~ ~ \$(real) run return 0")
			sampleLight()
			raw("\$" + macroCall(Spawn.spawnWithLight, "{skin:\"\$(skin)\",id:\"\$(id)\",facing:\"\$(facing)\"}"))
			returnValue(1)
		}

		// `apply_lit {real, skin, id, lit}`: draw `skin` over the `real` block, carrying a `lit`
		// block state. Context: at the block position.
		//
		// WHY THIS IS NOT JUST apply WITH A STATE IN THE NAME
		// A block_display's block_state is a compound: {id:"minecraft:furnace", properties:{...}}.
		// The id is a resource location and nothing else -- writing "minecraft:furnace[lit=true]"
		// into it puts square brackets in a resource location, which cannot resolve, so the display
		// spawns showing nothing at all. It is not an error you see in chat: the entity exists, it
		// is simply invisible, and the block looks like it lost its skin. That is exactly what
		// happened to the EU Generator when it was taught to glow.
		//
		// Only `lit` is supported, and only because two blocks in the pack want it. A general "any
		// properties" version would have to build the compound as text and the quoting does not
		// survive a macro argument.
		pack.define(RaLib.Skin.applyLit) {
			killSkins()
			raw("\$execute unless block ~ ~ ~ \$(real) run return 0")
			sampleLight()
			for (f in facings) raw(
				"\$execute if block ~ ~ ~ \$(real)[facing=$f] run " +
					macroCall(Spawn.spawnLitWithLight, "{skin:\"\$(skin)\",id:\"\$(id)\",facing:\"$f\",lit:\"\$(lit)\"}")
			)
			returnValue(1)
		}

		// `clear {id}`: remove the skin at the current position. Call from the block's break
		// handler, before the marker is killed.
		pack.define(RaLib.Skin.clear) { killSkins() }

		// `set_lit {id, lit}`: flip an existing skin's `lit` state in place. Context: at the block.
		//
		// WHY THIS EXISTS RATHER THAN CALLING apply_lit AGAIN
		// apply_lit kills the old display and summons a replacement. `kill` does not remove the
		// entity until the end of the tick, so for the rest of that tick there are TWO
		// block_displays at the same position with the same scale and the same depth -- which is
		// z-fighting -- and the handover pops a frame where the skin is simply gone. A machine that
		// changes lit state often does that every time it starts or stops, which is why the
		// Electric Furnace flickered and vanished on the fast modes.
		//
		// The display is already there and already correctly oriented. Only one string needs to
		// change, so change it on the entity that exists. No kill, no summon, no frame without a skin.
		//
		// Returns 1 if a display was found and updated, 0 if there was none -- the caller falls back
		// to apply_lit to build one from nothing.
		pack.define(RaLib.Skin.setLit) {
			raw("\$execute align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.\$(id),distance=..0.4,limit=1] run return 0")
			raw("\$execute align xyz positioned ~0.5 ~0.5 ~0.5 as @e[type=block_display,tag=ra.skin.\$(id),distance=..0.4] run data modify entity @s block_state.properties.lit set value \"\$(lit)\"")
			returnValue(1)
		}

		registerSpawn()
	}

	/**
	 * Anchored to the block centre, where ra_lib:skin/spawn stands its displays, so our own skin is
	 * at distance 0 and the neighbour's at a full 1.0. Any radius under 0.5 separates them cleanly;
	 * 0.4 leaves room for a caller that is not exactly centred. A pre-5.1.7 skin on the block's
	 * corner is removed by the 5.1.7 migration (ra.migrations), once per block.
	 */
	private fun Function.killSkins() {
		raw("\$execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=block_display,tag=ra.skin.\$(id),distance=..0.4]")
	}

	/**
	 * How bright to draw it. A block_display samples light at its OWN position, which is inside the
	 * block it is drawing, where the light is always zero -- so without an override every skin
	 * renders pitch black. It used to be hardcoded to block:0, which is right in daylight and wrong
	 * beside a torch: the real block would be lit and its skin would not.
	 *
	 * Sampled one block ABOVE, because that is the light a player standing next to the block sees,
	 * and because the block's own space is opaque.
	 */
	private fun Function.sampleLight() {
		execute {
			positioned(vec3(0, 1, 0).relative)
			run { function(Spawn.lightHere) }
		}
		raw("execute store result storage ra:temp skin.block_light int 1 run scoreboard players get #skin_light ${Obj.TEMP}")
	}
}
