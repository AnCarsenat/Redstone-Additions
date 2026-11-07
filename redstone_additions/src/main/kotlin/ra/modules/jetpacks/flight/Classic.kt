package ra.modules.jetpacks.flight

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Tags
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Kits

/** Classic mode: hold sneak to rise, nothing else changes. As the player wearing the jetpack, at the player. */
object Classic {
	val classic = ref(Ns.JETPACKS, "flight/classic")

	/** `predicate ra:is_sneaking`, owned by the ra namespace. */
	const val SNEAKING = "ra:is_sneaking"

	fun Ra.registerClassic() {
		pack.define(classic) { classic() }
	}

	private fun Function.classic() {
		// Gravity is only ever touched by hover mode; clean it up if the player just came back from there.
		raw("execute if entity @s[tag=${Jp.HOVER_ON}] run attribute @s minecraft:gravity modifier remove ${Jp.HOVER_MODIFIER}")
		tag(self()) { remove(Jp.HOVER_ON) }
		scoreboard.players.reset(self(), Jp.Y)
		scoreboard.players.reset(self(), Jp.GRAV)

		// Levitation is refreshed every tick while sneaking, so releasing sneak lets it lapse within
		// the second. Clearing it here keeps that from dragging on; normal gravity does the rest,
		// which is why classic never feels floaty. The engine has to be cut in the same breath, or
		// the last copy of the sample plays on for seconds.
		raw("execute unless predicate $SNEAKING run effect clear @s minecraft:levitation")
		execute {
			unlessCondition { predicate(SNEAKING) }
			run { function(Engine.soundOff) }
		}
		raw("execute unless predicate $SNEAKING run return 0")

		// Amplifier 2 is three blocks a second -- brisk, and still well under an elytra.
		raw("effect give @s minecraft:levitation 1 2 true")
		// Lift kit: amplifier 5, about six blocks a second. Given after the stock effect so it
		// replaces it -- a higher amplifier always wins.
		raw("execute if entity @s[tag=${Kits.lift.active}] run effect give @s minecraft:levitation 1 5 true")

		// Effects only once the player is off the ground: sneak-walking is not flying.
		raw("execute if block ~ ~-0.1 ~ #minecraft:air run particle minecraft:campfire_cosy_smoke ~ ~-0.15 ~ 0.15 0.05 0.15 0.01 3")
		raw("execute if block ~ ~-0.1 ~ #minecraft:air run particle minecraft:flame ~ ~-0.1 ~ 0.12 0.02 0.12 0.005 2")
		execute {
			ifCondition { block(vec3(0.0, -0.1, 0.0).relative, Tags.Block.AIR) }
			run { function(Engine.sound) }
		}
		execute {
			unlessCondition { block(vec3(0.0, -0.1, 0.0).relative, Tags.Block.AIR) }
			run { function(Engine.soundOff) }
		}

		function(Engine.fuel)
	}
}
