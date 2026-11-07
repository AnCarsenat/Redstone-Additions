package ra.modules.jetpacks.mode

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Jp.SELF_IF_SOUND
import ra.modules.jetpacks.Jp.tmp
import ra.modules.jetpacks.flight.Engine

/**
 * The three player switches behind /trigger ra.jp.mode, ra.jp.sound and ra.jp.power. Each runs
 * as the player who fired the trigger, consumes it, re-enables it and flips one score.
 *
 * The old value is always read first: writing the state and then testing it again would flip it
 * straight back.
 */
object Modes {
	val toggle = ref(Ns.JETPACKS, "mode/toggle")
	val soundToggle = ref(Ns.JETPACKS, "mode/sound_toggle")
	val powerToggle = ref(Ns.JETPACKS, "mode/power_toggle")

	fun Ra.registerModes() {
		// Flip between classic and hover.
		pack.define(toggle) {
			flip(Jp.MODE, Jp.STATE, "#jp.state")
			// Leaving hover has to give gravity back before the next tick runs.
			function(Engine.stop)
			raw("""execute if score @s ${Jp.STATE} matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"hover",color:"aqua"},{text:" — sneak and look up or down",color:"dark_gray"}]""")
			raw("""execute unless score @s ${Jp.STATE} matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"classic",color:"green"},{text:" — sneak to rise",color:"dark_gray"}]""")
			click()
		}

		// Mute or unmute the engine loop for one player.
		pack.define(soundToggle) {
			flip(Jp.SOUND, Jp.MUTE, "#jp.mute")
			// Muting has to kill the sample that is already playing, not just stop new ones.
			execute {
				ifCondition { score(self(), Jp.MUTE) equalTo 1 }
				at(self())
				run { function(Engine.soundOff) }
			}
			raw("""execute if score @s ${Jp.MUTE} matches 1 run title @s actionbar [{text:"Jetpack sound: ",color:"gray"},{text:"off",color:"red"}]""")
			raw("""execute unless score @s ${Jp.MUTE} matches 1 run title @s actionbar [{text:"Jetpack sound: ",color:"gray"},{text:"on",color:"green"}]""")
			click()
		}

		// Switch the whole jetpack off, or back on, for one player. Off means off: no hover, no
		// sneak thrust, no fuel burn, no particles. The chestplate keeps its components, so nothing
		// has to be re-fitted afterwards.
		pack.define(powerToggle) {
			flip(Jp.POWER, Jp.OFF, "#jp.off")
			// Switching off has to give gravity back before the next tick runs.
			execute {
				ifCondition { score(self(), Jp.OFF) equalTo 1 }
				run { function(Engine.stop) }
			}
			raw("""execute if score @s ${Jp.OFF} matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"off",color:"red"},{text:" — /trigger ra.jp.power to switch it back on",color:"dark_gray"}]""")
			raw("""execute unless score @s ${Jp.OFF} matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"on",color:"green"}]""")
			click()
		}
	}

	/** Consume [trigger], re-enable it, and flip [state] between 0 and 1 via the scratch [old]. */
	private fun Function.flip(trigger: String, state: String, old: String) {
		scoreboard.players.set(self(), trigger, 0)
		scoreboard.players.enable(self(), trigger)

		scoreboard.players.set(tmp(old), Obj.TEMP, 0)
		raw("execute if score @s $state matches 1 run scoreboard players set $old ${Obj.TEMP} 1")

		raw("execute if score $old ${Obj.TEMP} matches 0 run scoreboard players set @s $state 1")
		raw("execute if score $old ${Obj.TEMP} matches 1 run scoreboard players set @s $state 0")
	}

	private fun Function.click() = raw("playsound minecraft:block.lever.click player $SELF_IF_SOUND ~ ~ ~ 0.6 1.4")
}
