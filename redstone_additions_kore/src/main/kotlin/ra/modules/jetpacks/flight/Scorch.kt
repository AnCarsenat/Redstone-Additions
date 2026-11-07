package ra.modules.jetpacks.flight

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Jp.PARTICLE_VIEWERS
import ra.modules.jetpacks.Jp.tmp

/**
 * Scorch kit: set fire to whatever is under the exhaust. Plus [airborne], the ground test it and
 * the Thruster share.
 */
object Scorch {
	val scorch = ref(Ns.JETPACKS, "flight/scorch")
	val scorchOne = ref(Ns.JETPACKS, "flight/scorch_one")
	val scorchParticles = ref(Ns.JETPACKS, "flight/scorch_particles")
	val airborne = ref(Ns.JETPACKS, "flight/airborne")

	private val t = Obj.TEMP

	/** Hitbox corners and centre: a player is 0.6 blocks wide, so the corners sit at +/-0.3. */
	private val footprint = listOf("~ ~-0.1 ~", "~0.3 ~-0.1 ~0.3", "~0.3 ~-0.1 ~-0.3", "~-0.3 ~-0.1 ~0.3", "~-0.3 ~-0.1 ~-0.3")

	fun Ra.registerScorch() {
		pack.define(scorch) { scorch() }
		pack.define(scorchOne) { scorchOne() }
		pack.define(scorchParticles) { scorchParticles() }
		pack.define(airborne) { airborne() }
	}

	/**
	 * Is this player actually off the ground? As the player, at the player. Writes #jp.airborne
	 * ra.temp: 1 airborne, 0 standing on something.
	 *
	 * FIVE SAMPLES, NOT ONE
	 * This used to be a single `block ~ ~-0.1 ~`, which asks what is under the player's exact
	 * centre. A player standing on the EDGE of a block has their centre over the drop while their
	 * feet are still firmly on the corner -- so the jetpack decided they were flying, and the Scorch
	 * kit started burning whatever was below a player who was, from their point of view, standing
	 * still on solid ground.
	 *
	 * Any one corner over something solid means supported. Vanilla decides the same way -- you do
	 * not fall while any part of you is over a block.
	 */
	private fun Function.airborne() {
		scoreboard.players.set(tmp("#jp.airborne"), t, 1)
		for (pos in footprint) raw("execute unless block $pos #minecraft:air run scoreboard players set #jp.airborne $t 0")
	}

	/**
	 * As the flying player, at the player.
	 *
	 * WHY NOTHING EVER CAUGHT FIRE (kept as a warning)
	 * The selector used to carry `type=!boat`. There has been no `minecraft:boat` entity type since
	 * 1.21.2 -- it was split into oak_boat, birch_boat and the rest -- and one unknown entity type
	 * makes the WHOLE selector fail to parse, so the command never ran. The particles are separate
	 * commands, which is exactly why the exhaust looked right while nothing under it ever burned.
	 *
	 * The exclusion list is now short and every type in it is confirmed to exist. The pack's own
	 * entities are excluded by `tag=!ra` rather than by naming marker, text_display, block_display
	 * and friends one at a time -- that tag is on everything this pack summons, so it covers types
	 * that do not exist yet. It matters more than it sounds: setting Fire on an item entity
	 * destroys it, and every custom block here is a marker with displays attached, so without it a
	 * player hovering over their own base would burn their machines down.
	 *
	 * Armour stands, item frames and paintings are spared by one guard line each in scorch_one. A
	 * wrong type in a SELECTOR kills the whole feature silently; a wrong type in a guard costs that
	 * one guard.
	 */
	private fun Function.scorch() {
		// In the air, and nothing else. Standing on the ground with the kit fitted should not set
		// fire to your own feet -- and "on the ground" has to include standing on the edge of a
		// block, which a single sample under the player's centre gets wrong. See [airborne].
		function(airborne)
		raw("execute if score #jp.airborne $t matches 0 run return 0")

		function(scorchParticles)

		// Fire is set every tick, but the direct damage lands on a cadence. At one tick apart it
		// would be twenty hits a second, which kills anything under a hovering player instantly and
		// makes the burning itself pointless. Every ten ticks is a pressure rather than a delete
		// button.
		raw("execute unless score @s ${Jp.SCORCH_CD} matches -2147483648.. run scoreboard players set @s ${Jp.SCORCH_CD} 0")
		scoreboard.players.remove(self(), Jp.SCORCH_CD, 1)
		scoreboard.players.set(tmp("#jp.hit"), t, 0)
		raw("execute if score @s ${Jp.SCORCH_CD} matches ..0 run scoreboard players set #jp.hit $t 1")
		raw("execute if score #jp.hit $t matches 1 run scoreboard players set @s ${Jp.SCORCH_CD} 10")

		// Tagged so the damage can be attributed back to the pilot -- otherwise the kill belongs to
		// nobody and drops no experience.
		tag(self()) { add(Jp.SCORCHER) }
		execute {
			positioned(vec3(-1.5, -6.0, -1.5).relative)
			asTarget(allEntities {
				dx = 3.0
				dy = 5.8
				dz = 3.0
				tag = "!ra"
				type = !EntityTypes.PLAYER
				type = !EntityTypes.ITEM
				type = !EntityTypes.EXPERIENCE_ORB
			})
			run { function(scorchOne) }
		}
		tag(self()) { remove(Jp.SCORCHER) }
	}

	/**
	 * One thing caught under the exhaust. As that entity, at the pilot.
	 *
	 * Fire is set rather than only dealt as damage: sixty ticks of burning then behaves like any
	 * other fire, so something that steps out from under the jetpack keeps burning for a moment
	 * instead of stopping dead, and fire resistance, water and rain all still mean what they
	 * normally mean.
	 *
	 * `by @p[tag=ra.jp.scorcher]` attributes the hit to the pilot. The position here is still the
	 * player's -- `execute as` changes who is running, not where -- so the nearest player IS the
	 * one flying.
	 */
	private fun Function.scorchOne() {
		// Things players build and that would burn. One line each rather than four more entries in
		// the caller's selector -- see [scorch] for why.
		for (type in listOf("armor_stand", "item_frame", "glow_item_frame", "painting")) {
			raw("execute if entity @s[type=$type] run return 0")
		}

		raw("data modify entity @s Fire set value 60s")
		raw("execute if score #jp.hit $t matches 1 run damage @s 3 minecraft:on_fire by @p[tag=${Jp.SCORCHER}]")

		raw("execute at @s run particle minecraft:flame ~ ~0.5 ~ 0.5 0.6 0.5 0.04 14")
	}

	/**
	 * The Scorch kit's exhaust. At the flying player.
	 *
	 * Clean fire, spread wide. An earlier version threw about a hundred particles a tick including
	 * `explosion` billows and `large_smoke`, which read as a soot cloud rather than a jet -- the grey
	 * swamped the orange and there was enough of it to hide your own boots. `lava` is gone too: the
	 * popping blobs read as dripping stone, not flame.
	 *
	 * So: flame and small_flame only, and the width comes from the spread arguments rather than
	 * from the count. A wide, thin plume looks like thrust; a narrow dense one looks like a campfire.
	 */
	private fun Function.scorchParticles() {
		raw("particle minecraft:flame ~ ~-0.3 ~ 0.55 0.25 0.55 0.04 10 normal $PARTICLE_VIEWERS")
		raw("particle minecraft:small_flame ~ ~-0.5 ~ 0.65 0.4 0.65 0.04 8 normal $PARTICLE_VIEWERS")

		// Down the column it is burning, so the six block reach is visible rather than inferred from
		// something catching fire. Widening with depth, the way an exhaust plume actually spreads.
		raw("particle minecraft:flame ~ ~-2.0 ~ 0.75 0.8 0.75 0.02 7 normal $PARTICLE_VIEWERS")
		raw("particle minecraft:small_flame ~ ~-4.5 ~ 0.95 1.2 0.95 0.02 6 normal $PARTICLE_VIEWERS")
	}
}
