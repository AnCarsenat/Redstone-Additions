package ra.modules.jetpacks

import io.github.ayfri.kore.arguments.ARMOR
import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.selector.scores
import io.github.ayfri.kore.arguments.scores.greaterThanOrEqualTo
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import io.github.ayfri.kore.arguments.scores.ScoreboardCriteria
import ra.api.Settings
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.modules.enchanting.hookEnchantRecipes
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Kits.registerKits
import ra.modules.jetpacks.flight.Classic.registerClassic
import ra.modules.jetpacks.flight.Engine
import ra.modules.jetpacks.flight.Engine.registerEngine
import ra.modules.jetpacks.flight.Hover.registerHover
import ra.modules.jetpacks.flight.Scorch.registerScorch
import ra.modules.jetpacks.flight.Thrust.registerThrust
import ra.modules.jetpacks.flight.TickPlayer
import ra.modules.jetpacks.flight.TickPlayer.registerTickPlayer
import ra.modules.jetpacks.kit.FitModifiers.registerFitModifiers
import ra.modules.jetpacks.kit.KitFitting
import ra.modules.jetpacks.kit.KitFitting.registerKitFitting
import ra.modules.jetpacks.kit.KitMenu
import ra.modules.jetpacks.kit.KitMenu.registerKitMenu
import ra.modules.jetpacks.mode.Modes
import ra.modules.jetpacks.mode.Modes.registerModes
import ra.core.moduleLoaded

/** Jetpacks (`ra_jetpacks`): chestplate upgrade kits. */
object JetpacksModule : GameplayModule {
	override val ns = Ns.JETPACKS
	override val title = "Jetpacks"

	/**
	 * Flight tuning read once per tick (see [tick]): settings key, scratch name, default, and the
	 * bound under which the default is restored.
	 */
	private data class Tuning(val key: String, val score: String, val default: Int, val invalidBelow: String)

	private val tuning = listOf(
		Tuning("jetpack_thrust", "#jp.cfg.k", 80, "..0"),
		Tuning("jetpack_speed_cap", "#jp.cfg.cap", 350, "..0"),
		Tuning("jetpack_deadzone", "#jp.cfg.dead", 25, "..-1"),
	)

	override fun Ra.register() {
		registerKits()
		registerFitModifiers()
		registerKitFitting()
		registerKitMenu()
		registerModes()
		registerEngine()
		registerHover()
		registerClassic()
		registerThrust()
		registerScorch()
		registerTickPlayer()

		// After ra_infinite's list and ahead of ra_interactive's, as in the beet tag: see hookEnchantRecipes.
		pack.hookEnchantRecipes(Kits.enchantRecipes)

		// Right-clicking any kit. The kit is a consumable, so this fires while the button is held.
		pack.advancement("kit_use") {
			namespace = ns.id
			criteria {
				usingItem("requirement") {
					item {
						items = listOf(Items.FIREWORK_STAR)
						predicates { customData { this["ra"] = nbt { this["jetpack_kit"] = true } } }
					}
				}
			}
			rewards { function = KitFitting.onUse }
		}

		pack.define(ref(ns, "items/give_all")) {
			function(Kits.giveIron)
			function(Kits.giveInfinite)
			for (u in Kits.upgrades) function(u.give)
		}

		bundles += GiveBundle(
			id = "jetpacks",
			title = "Jetpacks Bundle",
			lore = "Chestplate upgrade kits",
			ns = ns,
			items = listOf(Kits.withoutSoundHint(Kits.ironKit).stackNbt(), Kits.withoutSoundHint(Kits.infiniteKit).stackNbt()) +
				Kits.upgrades.map { it.item.stackNbt() },
		)

		pack.define(load) { load() }
		pack.define(tick!!) { tick() }
	}

	private fun Function.load() {
		// The four jetpack triggers are NOT enabled here. flight/tick_player enables them for anyone
		// actually wearing a jetpack, every tick, so enabling them for everyone at load only put four
		// names in the /trigger completion of every player who has never seen one.
		scoreboard.objectives.add(Jp.MODE, ScoreboardCriteria.TRIGGER)
		scoreboard.objectives.add(Jp.STATE)
		scoreboard.objectives.add(Jp.FUEL)
		scoreboard.objectives.add(Jp.Y)
		scoreboard.objectives.add(Jp.SCORCH_CD)
		scoreboard.objectives.add(Jp.X)
		scoreboard.objectives.add(Jp.Z)
		scoreboard.objectives.add(Jp.KITS, ScoreboardCriteria.TRIGGER)
		scoreboard.objectives.add(Jp.VX)
		scoreboard.objectives.add(Jp.VZ)
		scoreboard.objectives.add(Jp.GRAV)
		scoreboard.objectives.add(Jp.AIR)
		scoreboard.objectives.add(Jp.OFF)
		scoreboard.objectives.add(Jp.POWER, ScoreboardCriteria.TRIGGER)
		scoreboard.objectives.add(Jp.SND)
		scoreboard.objectives.add(Jp.MUTE)
		scoreboard.objectives.add(Jp.SOUND, ScoreboardCriteria.TRIGGER)

		moduleLoaded("Jetpacks loaded!", "aqua")
	}

	/**
	 * Drive every worn jetpack.
	 *
	 * Every dispatch here is `as @s at @s`. `as` alone changes who runs the command but not where
	 * it runs, so the particles and sounds inside these functions were being played at the command
	 * origin instead of at the player, and nobody ever saw or heard them.
	 */
	private fun Function.tick() {
		// Flight tuning, read ONCE per tick rather than per flying player. ra_settings:get is a
		// macro, and a macro instantiation in a path that runs for every player in the air three
		// times over is a cost worth paying once instead.
		for (t in tuning) execute {
			storeResult { score(literal(t.score), Obj.TEMP) }
			run { function(Settings.get, nbt { this["key"] = t.key; this["default"] = t.default }) }
		}
		for (t in tuning) raw("execute if score ${t.score} ${Obj.TEMP} matches ${t.invalidBelow} run scoreboard players set ${t.score} ${Obj.TEMP} ${t.default}")

		// Kit right-click bookkeeping: a player who stopped clicking loses the held tag, so the next
		// click counts as a new one.
		raw("tag @a[tag=${Jp.KIT_ACTIVE},tag=!${Jp.KIT_CLICKED}] remove ${Jp.KIT_ACTIVE}")
		raw("tag @a[tag=${Jp.KIT_CLICKED}] remove ${Jp.KIT_CLICKED}")

		// Mode switches (/trigger ra.jp.mode), engine sound (ra.jp.sound), whole jetpack on/off
		// (ra.jp.power) and the upgrade menu (ra.jp.kits).
		for ((trigger, fn) in listOf(Jp.MODE to Modes.toggle, Jp.SOUND to Modes.soundToggle, Jp.POWER to Modes.powerToggle, Jp.KITS to KitMenu.menuAction)) {
			execute {
				asTarget(allPlayers { scores { trigger greaterThanOrEqualTo 1 } })
				at(self())
				run { function(fn) }
			}
		}

		// Anyone wearing a jetpack chestplate.
		execute {
			asTarget(allPlayers())
			at(self())
			ifCondition { selfItem(ARMOR.CHEST) { this["jetpack"] = true } }
			run { function(TickPlayer.tickPlayer) }
		}

		// Anyone who was wearing one a moment ago and is not now: put gravity back.
		execute {
			asTarget(allPlayers { tag = Jp.ON })
			at(self())
			unlessCondition { selfItem(ARMOR.CHEST) { this["jetpack"] = true } }
			run { function(Engine.stop) }
		}
	}
}
