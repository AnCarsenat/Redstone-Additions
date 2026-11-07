package ra.modules.jetpacks.flight

import io.github.ayfri.kore.arguments.types.literals.self
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
import ra.modules.jetpacks.Jp.SELF_IF_SOUND
import ra.modules.jetpacks.Jp.SOUND_LISTENERS

/** The engine sound, fuel, and handing the player back to vanilla. */
object Engine {
	val sound = ref(Ns.JETPACKS, "flight/sound")
	val soundOff = ref(Ns.JETPACKS, "flight/sound_off")
	val stop = ref(Ns.JETPACKS, "flight/stop")
	val fuel = ref(Ns.JETPACKS, "flight/fuel")
	val outOfFuel = ref(Ns.JETPACKS, "flight/out_of_fuel")

	private const val ENGINE = "minecraft:item.elytra.flying"

	fun Ra.registerEngine() {
		pack.define(sound) { sound() }
		pack.define(soundOff) { soundOff() }
		pack.define(stop) { stop() }
		pack.define(fuel) { fuel() }
		pack.define(outOfFuel) { outOfFuel() }
	}

	/**
	 * Keep the engine loop running. As the player, at the player. Called on every tick the jetpack
	 * is carrying the player -- hovering, climbing, sinking or thrusting. Callers decide when it
	 * runs; this only keeps it going.
	 *
	 * item.elytra.flying is a long sample, so exactly one instance exists at a time and it is
	 * replaced once a second. Stopping it is [soundOff]'s job.
	 */
	private fun Function.sound() {
		raw("execute if score @s ${Jp.MUTE} matches 1 run return 0")

		tag(self()) { add(Jp.SOUND_ON) }

		raw("execute unless score @s ${Jp.SND} matches 0.. run scoreboard players set @s ${Jp.SND} 19")
		scoreboard.players.add(self(), Jp.SND, 1)
		raw("execute unless score @s ${Jp.SND} matches 20.. run return 0")
		scoreboard.players.set(self(), Jp.SND, 0)

		raw("stopsound @a[distance=..20] player $ENGINE")
		raw("playsound $ENGINE player $SOUND_LISTENERS ~ ~ ~ 0.35 0.6")
	}

	/**
	 * Cut the engine loop. As the player, at the player.
	 *
	 * Called the moment the jetpack stops carrying the player -- sneak released in classic, feet on
	 * the ground in hover, chestplate off, out of fuel, switched off. Without this the last copy of
	 * a long sample keeps playing for seconds.
	 */
	private fun Function.soundOff() {
		raw("execute unless entity @s[tag=${Jp.SOUND_ON}] run return 0")

		tag(self()) { remove(Jp.SOUND_ON) }
		scoreboard.players.reset(self(), Jp.SND)
		raw("stopsound @a[distance=..20] player $ENGINE")
	}

	/** Hand the player back to vanilla physics. As the player. */
	private fun Function.stop() {
		raw("execute if entity @s[tag=${Jp.HOVER_ON}] run attribute @s minecraft:gravity modifier remove ${Jp.HOVER_MODIFIER}")
		raw("execute if entity @s[tag=${Jp.SPEED_ON}] run attribute @s minecraft:sneaking_speed modifier remove ${Jp.SNEAK_MODIFIER}")
		raw("effect clear @s minecraft:levitation")
		raw("effect clear @s minecraft:slow_falling")

		tag(self()) { remove(Jp.HOVER_ON) }
		tag(self()) { remove(Jp.SPEED_ON) }
		tag(self()) { remove(Jp.ON) }
		for (obj in listOf(Jp.Y, Jp.X, Jp.Z, Jp.VX, Jp.VZ)) scoreboard.players.reset(self(), obj)
		tag(self()) { remove(Jp.THRUSTING) }
		for (obj in listOf(Jp.GRAV, Jp.SND, Jp.AIR)) scoreboard.players.reset(self(), obj)

		// Cutting the jetpack has to cut the sound with it, or the engine keeps roaring for seconds
		// after gravity is back. Unconditional here rather than via sound_off, since stop is the
		// catch-all path.
		tag(self()) { remove(Jp.SOUND_ON) }
		raw("execute at @s run stopsound @a[distance=..20] player $ENGINE")
	}

	/**
	 * Burn coal for an iron jetpack: one per two minutes of flight. As the player, only on ticks
	 * where the jetpack actually pushed.
	 */
	private fun Function.fuel() {
		raw("""execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack_tier:"infinite"}}] run return 0""")

		scoreboard.players.add(self(), Jp.FUEL, 1)
		raw("execute unless score @s ${Jp.FUEL} matches 2400.. run return 0")
		scoreboard.players.set(self(), Jp.FUEL, 0)

		// Which fuel is on hand is decided before anything is taken, so the clear cannot be mistaken
		// for a second piece of coal.
		val t = Obj.TEMP
		raw("scoreboard players set #jp.fueled $t 0")
		raw("execute if items entity @s container.* minecraft:coal run scoreboard players set #jp.fueled $t 1")
		raw("execute if score #jp.fueled $t matches 0 if items entity @s weapon.offhand minecraft:coal run scoreboard players set #jp.fueled $t 1")
		raw("execute if score #jp.fueled $t matches 0 if items entity @s container.* minecraft:charcoal run scoreboard players set #jp.fueled $t 2")
		raw("execute if score #jp.fueled $t matches 0 if items entity @s weapon.offhand minecraft:charcoal run scoreboard players set #jp.fueled $t 2")

		raw("execute if score #jp.fueled $t matches 1 run clear @s minecraft:coal 1")
		raw("execute if score #jp.fueled $t matches 2 run clear @s minecraft:charcoal 1")
		raw("execute if score #jp.fueled $t matches 1.. run playsound minecraft:block.furnace.fire_crackle player @s ~ ~ ~ 0.5 1")
		raw("execute if score #jp.fueled $t matches 1.. run return 1")

		function(outOfFuel)
	}

	/** No coal left. The jetpack cuts out until some turns up. As the player. */
	private fun Function.outOfFuel() {
		function(stop)
		tag(self()) { add(Jp.DRY) }

		raw("""title @s actionbar [{text:"Jetpack out of fuel",color:"red"},{text:" — carry coal",color:"gray"}]""")
		raw("playsound minecraft:block.fire.extinguish player $SELF_IF_SOUND ~ ~ ~ 0.8 0.6")
	}
}
