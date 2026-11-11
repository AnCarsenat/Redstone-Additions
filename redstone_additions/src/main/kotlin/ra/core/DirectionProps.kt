package ra.core

import io.github.ayfri.kore.functions.Function

/**
 * DIRECTION AND FACING PROPERTIES: which way something points.
 *
 * DIRECTION (`direction(name, default)`): one of the six faces, stored as the string the block
 * state uses: `"down"`, `"up"`, `"north"`, `"south"`, `"west"`, `"east"` ([DIRECTIONS]). The Data
 * Handler shows six buttons and writes one of those six constants, never typed text; a block
 * reads it with NBT matches against the same six (anything else is its default), so no value of
 * it is ever pasted.
 *
 * FACING (`facing(name, FacingSpec(...))`): a yaw and a pitch in whole degrees,
 * `{yaw:<int>, pitch:<int>}`, in Minecraft's sense: yaw 0 is south, 90 west, 180 (or -180) north,
 * -90 east, turning clockwise seen from above; a positive pitch tips the front down. This is the
 * yaw/pitch form of issue #166; a horizontal facing (north, south, east, west) is the yaw alone,
 * and a WORLD facing's editor has [North] [East] [South] [West] buttons that set it with pitch 0.
 *
 * ```kotlin
 * properties(block) {
 *     direction("facing", default = "south")
 *     facing("rotation", FacingSpec(frame = FacingFrame.LOCAL, steps = listOf(1, 15, 45)))
 * }
 * // use: readFacing("rotation", spec) -> scores #fc.yaw / #fc.pitch ra.temp
 * ```
 *
 * [FacingSpec]:
 *   yawMin/yawMax, pitchMin/pitchMax  the range, in degrees. A full turn of yaw (max - min = 360)
 *            wraps round when nudged past either end; pitch stops at its ends.
 *   grid     values must be multiples of it.
 *   steps    the nudge sizes the Data Handler offers (at most four, multiples of grid).
 *   frame    [FacingFrame]: LOCAL (turned from the block's own front, a display's tilt on its
 *            face) or WORLD (a compass facing, with the compass buttons).
 *   default  what placement writes and [Reset] puts back.
 *
 * Every value a player can give is an integer by the time it is stored (the number form, a nudge,
 * a compass button), and [readFacing] copies each part through a score, so it is always two ints.
 *
 * Generated into the schema as `fc:{min:[I;yaw,pitch],max:[I;yaw,pitch],grid,wrap,frame,
 * steps:[I;..],labels:[..],def:{yaw,pitch},frame_label}`; the editor is
 * tools/datahandler/props/DataHandlerFacing.kt.
 */
val DIRECTIONS = listOf("down", "up", "north", "south", "west", "east")

/** A direction property: one of [DIRECTIONS]. */
fun PropertiesBuilder.direction(
	name: String, default: String,
	readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null,
) {
	require(default in DIRECTIONS) { "$name: a direction's default must be one of $DIRECTIONS" }
	require(specs.none { it.name == name }) { "property $name declared twice" }
	specs += PropSpec(name, PropType.DIRECTION, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles, default = snbtString(default))
}

enum class FacingFrame(val code: Int, val label: String) {
	LOCAL(0, "from the block's front"),
	WORLD(1, "compass"),
}

class FacingSpec(
	val yawMin: Int = -180,
	val yawMax: Int = 180,
	val pitchMin: Int = -90,
	val pitchMax: Int = 90,
	val grid: Int = 1,
	val steps: List<Int> = listOf(1, 15, 45),
	val frame: FacingFrame = FacingFrame.WORLD,
	val defaultYaw: Int = 0,
	val defaultPitch: Int = 0,
) {
	init {
		require(yawMin in -360..360 && yawMax in -360..360 && yawMin <= yawMax && yawMax - yawMin <= 360) { "facing yaw range: within -360..360, at most one turn" }
		require(pitchMin in -90..90 && pitchMax in -90..90 && pitchMin <= pitchMax) { "facing pitch range: within -90..90" }
		require(grid >= 1) { "facing grid must be at least 1" }
		require(steps.size in 1..MAX_STEPS && steps.all { it > 0 && it % grid == 0 }) { "facing steps: 1 to $MAX_STEPS positive multiples of the grid" }
		require(defaultYaw in yawMin..yawMax && defaultPitch in pitchMin..pitchMax) { "facing default must be in range" }
		require(defaultYaw % grid == 0 && defaultPitch % grid == 0) { "facing default must be on the grid" }
	}

	/** Yaw goes round: nudging past one end comes back in at the other. */
	val wraps get() = yawMax - yawMin == 360

	fun defaultSnbt() = "{yaw:$defaultYaw,pitch:$defaultPitch}"

	fun snbtFields(): String = buildList {
		add("min:[I;$yawMin,$pitchMin]")
		add("max:[I;$yawMax,$pitchMax]")
		add("grid:$grid")
		add("wrap:${if (wraps) 1 else 0}b")
		add("frame:${frame.code}")
		add("steps:[I;${steps.joinToString(",")}]")
		add("labels:[${steps.joinToString(",") { "\"$it°\"" }}]")
		add("def:${defaultSnbt()}")
		add("frame_label:\"${frame.label}\"")
	}.joinToString(",", "fc:{", "}")

	companion object {
		const val MAX_STEPS = 4

		/** Compass yaws for a WORLD facing's buttons, in Minecraft's sense. */
		val COMPASS = listOf("North" to 180, "East" to -90, "South" to 0, "West" to 90)
	}
}

/** A facing property: see [FacingSpec]. Its default is the spec's. */
fun PropertiesBuilder.facing(
	name: String, spec: FacingSpec,
	readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null,
) {
	require(specs.none { it.name == name }) { "property $name declared twice" }
	specs += PropSpec(name, PropType.FACING, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles, default = spec.defaultSnbt(), facing = spec)
}

/**
 * Use time, as the block's marker: `data.properties.<name>` into the scores `#fc.yaw` and
 * `#fc.pitch` (ra.temp), each clamped to the spec's range. A missing property, or one without
 * both parts, reads as the default. The result is two integers whatever the property holds.
 */
fun Function.readFacing(name: String, spec: FacingSpec) {
	val t = Obj.TEMP
	val p = "data.properties.$name"
	raw("execute store success score #fc.ok $t if data entity @s $p.yaw if data entity @s $p.pitch")
	fun part(part: String, def: Int, lo: Int, hi: Int) {
		val s = "#fc.$part"
		raw("scoreboard players set $s $t $def")
		raw("execute if score #fc.ok $t matches 1 store result score $s $t run data get entity @s $p.$part")
		raw("execute if score $s $t matches ..${lo - 1} run scoreboard players set $s $t $lo")
		raw("execute if score $s $t matches ${hi + 1}.. run scoreboard players set $s $t $hi")
	}
	part("yaw", spec.defaultYaw, spec.yawMin, spec.yawMax)
	part("pitch", spec.defaultPitch, spec.pitchMin, spec.pitchMax)
}
