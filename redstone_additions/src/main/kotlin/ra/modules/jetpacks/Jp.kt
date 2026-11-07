package ra.modules.jetpacks

import io.github.ayfri.kore.arguments.ItemSlot
import io.github.ayfri.kore.arguments.components.ItemPredicate
import io.github.ayfri.kore.arguments.components.item.customData
import io.github.ayfri.kore.arguments.components.itemPredicate
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.ExecuteCondition
import io.github.ayfri.kore.generated.ItemComponentTypes
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtCompoundBuilder
import ra.core.Obj

/**
 * Identifiers of ra_jetpacks. Everything the jetpack remembers about a player lives on the
 * player (scores, tags); everything about the jetpack itself lives on the chestplate.
 */
object Jp {
	// Triggers. Players switch flight mode with /trigger ra.jp.mode, mute with ra.jp.sound, switch
	// the whole jetpack with ra.jp.power and open the upgrade menu with ra.jp.kits.
	const val MODE = "ra.jp.mode"
	const val SOUND = "ra.jp.sound"
	const val POWER = "ra.jp.power"
	const val KITS = "ra.jp.kits"

	/** 0 = classic, 1 = hover. */
	const val STATE = "ra.jp.state"

	/** Ticks of powered flight since the last coal was burned. */
	const val FUEL = "ra.jp.fuel"

	/** Hover servo: last sampled Y in thousandths of a block. */
	const val Y = "ra.jp.y"

	/** Scorch kit: ticks until the next direct hit, so burning is continuous while the damage is not twenty times a second. */
	const val SCORCH_CD = "ra.jp.scorch_cd"

	/** Thruster kit: last sampled X/Z in thousandths, so the push is measured from how far the player actually moved. */
	const val X = "ra.jp.x"
	const val Z = "ra.jp.z"

	/** Thruster kit: the SMOOTHED horizontal speed the push is built from. */
	const val VX = "ra.jp.vx"
	const val VZ = "ra.jp.vz"

	/** The thruster tier currently written onto the gravity attribute. */
	const val GRAV = "ra.jp.grav"

	/** 1 once the player has actually left the ground, so touching down can be told apart from standing there. */
	const val AIR = "ra.jp.air"

	/** Master switch: a player with it set wears a jetpack that does nothing at all. */
	const val OFF = "ra.jp.off"

	/** Engine-loop timer, and the mute flag behind /trigger ra.jp.sound. */
	const val SND = "ra.jp.snd"
	const val MUTE = "ra.jp.mute"

	// Player tags.
	const val ON = "ra.jetpack_on"
	const val HOVER_ON = "ra.jp.hover_on"
	const val SPEED_ON = "ra.jp.speed_on"
	const val THRUSTING = "ra.jp.thrusting"
	const val SOUND_ON = "ra.jp.sound_on"
	const val DRY = "ra.jp.dry"
	const val KIT_CLICKED = "ra.jp.kit_clicked"
	const val KIT_ACTIVE = "ra.jp.kit_active"
	const val SCORCHER = "ra.jp.scorcher"

	/** Gravity / sneaking-speed attribute modifier ids. */
	const val HOVER_MODIFIER = "ra.jetpack.hover"
	const val SNEAK_MODIFIER = "ra.jetpack.sneak"

	/** `#<name> ra.temp`. */
	fun tmp(name: String) = literal(name)

	const val SELF_IF_SOUND = "@s[scores={${Obj.USER_SOUND}=1..}]"
}

/** An item predicate on `custom_data~{ra:{...}}`. */
fun raDataPredicate(ra: NbtCompoundBuilder.() -> Unit): ItemPredicate = itemPredicate {
	customData { this["ra"] = nbt(ra) }
	setPartial(ItemComponentTypes.CUSTOM_DATA)
}

/** `items entity @s <slot> *[minecraft:custom_data~{ra:{...}}]`. */
fun ExecuteCondition.selfItem(slot: ItemSlot, ra: NbtCompoundBuilder.() -> Unit) = items(self(), slot, raDataPredicate(ra))
