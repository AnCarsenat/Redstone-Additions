package ra.lib.skin

import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.lib.fake
import ra.lib.lib
import ra.lib.light.LightPredicates

/** The skin summons, the light-merging steps in front of them, and `light_here`. */
internal object Spawn {
	val spawn = lib("skin/spawn")
	val spawnStatic = lib("skin/spawn_static")
	val spawnLit = lib("skin/spawn_lit")
	val spawnWithLight = lib("skin/spawn_with_light")
	val spawnStaticWithLight = lib("skin/spawn_static_with_light")
	val spawnLitWithLight = lib("skin/spawn_lit_with_light")
	val lightHere = lib("skin/light_here")

	/**
	 * Tags and geometry every skin display shares; [state] is the `block_state` compound.
	 *
	 * Scaled over 1 and pulled back by half the excess, so the skin encloses the real block and
	 * their surfaces never share a plane. Sitting exactly on 1.0 would z-fight with the block
	 * underneath.
	 *
	 * HOW FAR OVER 1, AND WHY IT IS NOT 1.004
	 * The margin was 1.004 -- two thousandths of a block clear on each face -- which is
	 * geometrically separate and practically not. Minecraft's depth buffer loses precision with
	 * distance, so two surfaces that close resolve to the same depth value a few chunks out and the
	 * skin flickers against the block underneath.
	 *
	 * 1.02 gives a hundredth of a block per face: five times the separation, and still only about a
	 * sixth of a pixel of overhang on a sixteen-pixel texture, so nothing looks fat. Where a
	 * neighbour is solid the overhang is buried inside it and never drawn; where it is air, a sixth
	 * of a pixel is invisible. The two surfaces are also parallel rather than coplanar, so the
	 * overhang cannot start a new fight with the neighbour's face.
	 *
	 * translation is always -scale/2: the model runs 0..scale from the display's own position, so
	 * half of it has to come back to centre on the block.
	 *
	 * A block_display has no collision and no interaction box, so the real block behind it still
	 * takes right-clicks, hopper insertion and comparator reads.
	 *
	 * `brightness` is set because a display samples the light at its own position, and its own
	 * position is inside an opaque block, where the light level is zero. Without this every skin
	 * renders pitch black. sky:15 lights it as if it were an ordinary surface block, so it still
	 * dims at night instead of glowing; block is the level light_here sampled above the block.
	 *
	 * ANCHORED AT THE BLOCK CENTRE, NOT THE CORNER
	 * A block_display draws its block from its own position outward, so the obvious thing is to
	 * stand it on the block's minimum corner. That is what this used to do, and it made every "is
	 * this skin mine?" test ambiguous: a marker sits at the block CENTRE, and from a centre all
	 * eight surrounding corners are the same 0.866 away -- our own skin and the neighbours' at +x,
	 * +y and +z included. No radius could separate them, so adjacent skinned blocks deleted each
	 * other's appearance and then each decided the other's leftover skin was its own.
	 *
	 * Standing the display at the centre instead and pushing the model back by half a block through
	 * the transformation puts it in exactly the same place on screen, while making the entity sit
	 * where the marker sits: distance 0 for ours, a clean 1.0 for the neighbour's. The translation
	 * is -(0.5 + 0.01): half a block to recentre, plus the overshoot that stops z-fighting.
	 */
	private fun Function.summonSkin(state: String) = raw(
		"\$execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ " +
			"{Tags:[\"ra\",\"ra.display\",\"ra.skin\",\"ra.skin.\$(id)\"],brightness:{sky:15,block:\$(block_light)},block_state:$state," +
			"transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.51f,-0.51f,-0.51f],scale:[1.02f,1.02f,1.02f]}}"
	)

	/** A macro cannot read a score, and the caller's own arguments are already a macro substitution, so the light has to join them in storage before the summon. */
	private fun Function.mergeIntoSkin(vararg keys: String) {
		for (k in keys) raw("\$data modify storage ra:temp skin.$k set value \"\$($k)\"")
	}

	fun Ra.registerSpawn() {
		// `spawn {skin, id, facing, block_light}`: internal, summon one oriented skin.
		pack.define(spawn) { summonSkin("{id:\"\$(skin)\",properties:{facing:\"\$(facing)\"}}") }

		// `spawn_static {skin, id, block_light}`: internal, summon one unoriented skin.
		pack.define(spawnStatic) { summonSkin("{id:\"\$(skin)\"}") }

		// `spawn_lit {skin, id, facing, lit, block_light}`: internal, summon one oriented skin that
		// also carries a `lit` state. See apply_lit for why the state cannot simply be written into
		// the block name.
		//
		// `lit` is a string here because block state values in a block_state compound are always
		// strings -- lit:true would be a boolean and would not match.
		pack.define(spawnLit) { summonSkin("{id:\"\$(skin)\",properties:{facing:\"\$(facing)\",lit:\"\$(lit)\"}}") }

		// `*_with_light`: internal, merge the sampled light level in, then spawn.
		pack.define(spawnWithLight) {
			mergeIntoSkin("skin", "id", "facing")
			function(spawn, Storage.TEMP, "skin")
		}
		pack.define(spawnStaticWithLight) {
			mergeIntoSkin("skin", "id")
			function(spawnStatic, Storage.TEMP, "skin")
		}
		pack.define(spawnLitWithLight) {
			mergeIntoSkin("skin", "id", "facing", "lit")
			function(spawnLit, Storage.TEMP, "skin")
		}

		pack.define(lightHere) { lightHere() }
	}

	/**
	 * `light_here`: light level at the current position into #skin_light ra.temp (0-15).
	 *
	 * WHY A BINARY SEARCH
	 * There is no command that reads a light level. A location predicate can TEST one against a
	 * range, so the level has to be found by asking yes/no questions, and fifteen of them per skin
	 * is a lot of predicate evaluation for a cosmetic number. Four questions cover 0-15 exactly,
	 * which is what a binary search costs.
	 *
	 * Called where the light should be sampled -- which is NOT where the display stands. A
	 * block_display sits inside the block it is drawing, where the light is always zero, and that
	 * is why every skin renders pitch black without a brightness override. The caller positions
	 * this one block above instead: the light a player standing next to the block actually sees.
	 */
	private fun Function.lightHere() {
		val light = fake("#skin_light")
		scoreboard.players.set(light, Obj.TEMP, 0)

		execute {
			ifCondition { predicate(LightPredicates.atLeast(8)) }
			run { scoreboard.players.add(light, Obj.TEMP, 8) }
		}

		// Each round halves the step: for every value reachable so far (highest first, as the beet
		// file listed them), test whether the light reaches value + step.
		for (step in listOf(4, 2, 1)) {
			for (base in (0..15 step step * 2).reversed()) {
				execute {
					ifCondition { score(light, Obj.TEMP) equalTo base }
					ifCondition { predicate(LightPredicates.atLeast(base + step)) }
					run { scoreboard.players.add(light, Obj.TEMP, step) }
				}
			}
		}
	}
}
