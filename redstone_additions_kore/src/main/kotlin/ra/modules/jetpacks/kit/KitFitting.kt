package ra.modules.jetpacks.kit

import io.github.ayfri.kore.arguments.ARMOR
import io.github.ayfri.kore.arguments.WEAPON
import io.github.ayfri.kore.arguments.components.itemPredicate
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtString
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Jp.PARTICLE_VIEWERS
import ra.modules.jetpacks.Jp.SELF_IF_SOUND
import ra.modules.jetpacks.Jp.SOUND_LISTENERS
import ra.modules.jetpacks.Jp.tmp
import ra.modules.jetpacks.Kits
import ra.modules.jetpacks.selfItem

/**
 * Fitting kits onto the worn chestplate. All of these run as the player, kit in the main hand.
 *
 * THE UPGRADE LIVES ON THE CHESTPLATE
 * It used to live on the player, as a tag, on the reasoning that every other jetpack setting
 * already does and that an item modifier cannot merge into an existing component. The first half
 * was true and the second was the wrong conclusion: a tag on the player is not a property of the
 * jetpack, so the upgrades followed you to a different chestplate, survived losing the jetpack
 * entirely, and -- the way it actually showed up -- made every kit report itself already fitted
 * for ever after the first one.
 *
 * The merge problem is real, and the answer is to not merge: read the chestplate's whole state,
 * work out the new whole state, and write that. See [FitModifiers].
 */
object KitFitting {
	val onUse = ref(Ns.JETPACKS, "kit/on_use")
	val applyIron = ref(Ns.JETPACKS, "kit/apply_iron")
	val applyInfinite = ref(Ns.JETPACKS, "kit/apply_infinite")
	/** Macro `{bit, key, kit, name, hint}`. */
	val applyUpgrade = ref(Ns.JETPACKS, "kit/apply_upgrade")
	/** Macro `{tier, n}`: stamp the chestplate with the state named by tier and n. */
	val applyFit = ref(Ns.JETPACKS, "kit/apply_fit")
	val readFitted = ref(Ns.JETPACKS, "kit/read_fitted")
	val readFittedTierOnly = ref(Ns.JETPACKS, "kit/read_fitted_tier_only")
	val writeFitted = ref(Ns.JETPACKS, "kit/write_fitted")
	val alreadyFitted = ref(Ns.JETPACKS, "kit/already_fitted")
	val needsJetpack = ref(Ns.JETPACKS, "kit/needs_jetpack")

	private val n = tmp("#jp.n")

	fun Ra.registerKitFitting() {
		pack.define(onUse) { onUse() }
		pack.define(applyIron) { applyIron() }
		pack.define(applyInfinite) { applyInfinite() }
		pack.define(applyUpgrade) { applyUpgrade() }
		pack.define(applyFit) { raw("\$item modify entity @s armor.chest ${Ns.JETPACKS}:fit_\$(tier)_\$(n)") }
		pack.define(readFitted) { readFitted() }
		// The tier half of read_fitted, for callers that have already set #jp.n by hand.
		pack.define(readFittedTierOnly) { readTier() }
		pack.define(writeFitted) { writeFitted() }

		// Refusing rather than silently eating the kit: the upgrades do not stack, so a second one
		// would be spent for nothing.
		pack.define(alreadyFitted) {
			raw("""title @s actionbar [{text:"That upgrade is already fitted",color:"yellow"}]""")
			raw("playsound minecraft:block.fire.extinguish player $SELF_IF_SOUND ~ ~ ~ 0.6 0.9")
		}
		// Refuse an upgrade kit with nothing to upgrade.
		pack.define(needsJetpack) {
			raw("""title @s actionbar [{text:"Fit a jetpack to your chestplate first",color:"red"}]""")
			raw("playsound minecraft:block.fire.extinguish player $SELF_IF_SOUND ~ ~ ~ 0.6 0.6")
		}
	}

	/** A jetpack kit was right-clicked. As the player, from the `kit_use` advancement. */
	private fun Function.onUse() {
		raw("advancement revoke @s only ${Ns.JETPACKS}:kit_use")

		// The kit is a consumable, so the trigger fires on every tick of the right-click and would
		// fit -- and eat -- one kit per tick. Same held/clicked pair the core tools use;
		// ra_jetpacks:tick clears the tags once the button is released.
		raw("tag @s add ${Jp.KIT_CLICKED}")
		raw("execute if entity @s[tag=${Jp.KIT_ACTIVE}] run return fail")
		raw("tag @s add ${Jp.KIT_ACTIVE}")

		execute {
			ifCondition { selfItem(WEAPON.MAINHAND) { this["jetpack_kit"] = true; this["tier"] = "iron" } }
			run { returnRun(applyIron) }
		}
		execute {
			ifCondition { selfItem(WEAPON.MAINHAND) { this["jetpack_kit"] = true; this["tier"] = "infinite" } }
			run { returnRun(applyInfinite) }
		}

		// Upgrade kits. They fit onto a jetpack that is already worn rather than onto a bare
		// chestplate, so they are checked after the two that create one.
		for (u in Kits.upgrades) execute {
			ifCondition { selfItem(WEAPON.MAINHAND) { this["jetpack_kit"] = true; this["upgrade"] = u.kit } }
			run {
				returnRun {
					function(applyUpgrade, nbt {
						this["bit"] = u.bit; this["key"] = u.key; this["kit"] = u.kit; this["name"] = u.name; this["hint"] = u.hint
					})
				}
			}
		}
	}

	/**
	 * Fit an iron jetpack to the chestplate the player is wearing. The chestplate keeps its own
	 * material, damage and enchantments: the kit only writes components onto it.
	 */
	private fun Function.applyIron() {
		val ok = tmp("#jp.ok")
		scoreboard.players.set(ok, Obj.TEMP, 1)
		execute {
			unlessCondition { items(self(), ARMOR.CHEST, itemPredicate()) }
			run { scoreboard.players.set(ok, Obj.TEMP, 0) }
		}
		execute {
			ifCondition { selfItem(ARMOR.CHEST) { this["jetpack"] = true } }
			run { scoreboard.players.set(ok, Obj.TEMP, 0) }
		}

		raw("""execute if score #jp.ok ${Obj.TEMP} matches 0 run title @s actionbar [{text:"Wear a chestplate without a jetpack first",color:"red"}]""")
		raw("execute if score #jp.ok ${Obj.TEMP} matches 0 run playsound minecraft:block.fire.extinguish player @s ~ ~ ~ 0.6 0.6")
		raw("execute if score #jp.ok ${Obj.TEMP} matches 0 run return 0")

		// fit_iron_0 rather than a modifier of its own: a fresh jetpack is the iron tier with no
		// upgrades, which is exactly state 0, and routing through the same generated files keeps
		// one source of truth for the lore. It also guarantees a newly fitted jetpack carries no
		// upgrade flags left over from whatever the chestplate was before.
		raw("item modify entity @s armor.chest ${Ns.JETPACKS}:fit_iron_0")
		raw("""clear @s *[minecraft:custom_data~{ra:{jetpack_kit:1b,tier:"iron"}}] 1""")

		raw("""title @s actionbar [{text:"Iron jetpack fitted",color:"green"},{text:" — sneak to fly",color:"gray"}]""")
		raw("particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.6 40 normal $PARTICLE_VIEWERS")
		raw("playsound minecraft:block.anvil.use player $SOUND_LISTENERS ~ ~ ~ 0.8 1.2")
		raw("playsound minecraft:entity.player.levelup player $SELF_IF_SOUND ~ ~ ~ 0.6 1.6")
	}

	/**
	 * Fit an infinite jetpack to the chestplate the player is wearing. Unlike the iron kit this
	 * one goes on over an existing jetpack, so an iron jetpack can be upgraded in place.
	 */
	private fun Function.applyInfinite() {
		raw("""execute unless items entity @s armor.chest * run title @s actionbar [{text:"Wear a chestplate first",color:"red"}]""")
		raw("execute unless items entity @s armor.chest * run return 0")

		// Read the fitted upgrades BEFORE the tier is rewritten, then write tier and upgrades
		// together. Applying the plain apply_infinite modifier here would set custom_data whole and
		// silently strip every kit the player had paid for -- upgrading in place is meant to keep
		// what you had.
		function(readFitted)
		data(Storage.JETPACKS) { modify("fit.tier") { set(NbtString("infinite")) } }
		raw("execute store result storage ra:jetpacks fit.n int 1 run scoreboard players get #jp.n ${Obj.TEMP}")
		function(applyFit, Storage.JETPACKS, "fit")
		raw("""clear @s *[minecraft:custom_data~{ra:{jetpack_kit:1b,tier:"infinite"}}] 1""")

		raw("""execute if score #jp.n ${Obj.TEMP} matches 1.. run title @s actionbar [{text:"Infinite jetpack fitted",color:"light_purple"},{text:" - fuel free, kits kept",color:"gray"}]""")
		raw("""execute if score #jp.n ${Obj.TEMP} matches ..0 run title @s actionbar [{text:"Infinite jetpack fitted",color:"light_purple"},{text:" - no fuel needed",color:"gray"}]""")
		raw("particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.8 60 normal $PARTICLE_VIEWERS")
		raw("playsound minecraft:block.beacon.activate player $SOUND_LISTENERS ~ ~ ~ 0.7 1.4")
		raw("playsound minecraft:entity.player.levelup player $SELF_IF_SOUND ~ ~ ~ 0.6 1.8")
	}

	/** Fit one upgrade kit. Macro `{bit, key, kit, name, hint}`. */
	private fun Function.applyUpgrade() {
		execute {
			unlessCondition { selfItem(ARMOR.CHEST) { this["jetpack"] = true } }
			run { returnRun(needsJetpack) }
		}

		raw("\$execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{\$(key):1b}}] run return run function ${alreadyFitted.asId()}")

		function(readFitted)
		raw("\$scoreboard players add #jp.n ${Obj.TEMP} \$(bit)")
		function(writeFitted)

		raw("\$clear @s *[minecraft:custom_data~{ra:{jetpack_kit:1b,upgrade:\"\$(kit)\"}}] 1")

		raw("\$title @s actionbar [{text:\"\$(name) kit fitted\",color:\"green\"},{text:\" - \$(hint)\",color:\"gray\"}]")
		raw("particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.6 40 normal $PARTICLE_VIEWERS")
		raw("playsound minecraft:block.anvil.use player $SOUND_LISTENERS ~ ~ ~ 0.8 1.4")
		raw("playsound minecraft:entity.player.levelup player $SELF_IF_SOUND ~ ~ ~ 0.6 1.8")
	}

	/**
	 * Which upgrades the worn chestplate carries into #jp.n ra.temp, as bits (1 Thruster, 2 Lift,
	 * 4 Scorch), and its tier into storage ra:jetpacks fit.tier.
	 *
	 * The chestplate is the record. Everything here is derived from it, so a jetpack handed to
	 * someone else arrives with its upgrades, and a player who takes one off stops having them.
	 */
	private fun Function.readFitted() {
		scoreboard.players.set(n, Obj.TEMP, 0)
		for (u in Kits.upgrades) execute {
			ifCondition { selfItem(ARMOR.CHEST) { this[u.key] = true } }
			run { scoreboard.players.add(n, Obj.TEMP, u.bit) }
		}
		readTier()
	}

	private fun Function.readTier() {
		data(Storage.JETPACKS) { modify("fit.tier") { set(NbtString("iron")) } }
		execute {
			ifCondition { selfItem(ARMOR.CHEST) { this["jetpack_tier"] = "infinite" } }
			run { data(Storage.JETPACKS) { modify("fit.tier") { set(NbtString("infinite")) } } }
		}
	}

	/** Write #jp.n ra.temp back onto the worn chestplate, through the matching [FitModifiers] file. */
	private fun Function.writeFitted() {
		function(readFittedTierOnly)
		raw("execute store result storage ra:jetpacks fit.n int 1 run scoreboard players get #jp.n ${Obj.TEMP}")
		function(applyFit, Storage.JETPACKS, "fit")
	}
}
