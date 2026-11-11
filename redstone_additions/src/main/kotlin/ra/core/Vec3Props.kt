package ra.core

import io.github.ayfri.kore.functions.Function

/**
 * VEC3 PROPERTIES: three integers `{x:<int>, y:<int>, z:<int>}`, a position or an offset.
 *
 * This is option S2 of the coordinates design note (planning/design/coordinates-input.md, #48):
 * a position is never text. Every value a player can give is an integer by the time it is stored
 * (the number form, a nudge, a pick), and a block that uses one copies each part through a score
 * ([readVec3]), which can only ever produce an integer, before anything is pasted. Nothing a player
 * typed reaches a command as text, and there is no `guardProperty` to forget.
 *
 * ```kotlin
 * val TARGET = Vec3Spec(min = Vec3i(-16), max = Vec3i(16), adminMax = Vec3i(64), adminMin = Vec3i(-64))
 * properties(block) { vec3("target", TARGET) }
 * // use: readVec3("target", TARGET, "storage ra:temp bd.pos") then a macro with $(x) $(y) $(z)
 * ```
 *
 * [Vec3Spec]:
 *   unit     how many steps make a block: 1 (whole blocks) or 16 (sixteenths, a display's offset).
 *            Values are integers in that unit.
 *   min/max  per axis, in the unit: what a player may set. For an ABSOLUTE frame they bound the
 *            distance from the block, not the world coordinate.
 *   adminMin/adminMax  per axis: what an admin may set (ADMINS BYPASS RANGE LIMITS, see [Admin]);
 *            also the technical cap [readVec3] clamps to at use time. Null: the players' range.
 *   grid     values must be multiples of it (a step grid); 1 takes any integer.
 *   frame    [Vec3Frame]: LOCAL (the block's own frame, a display's right / up / out), RELATIVE
 *            (a world offset from the block, `~`), ABSOLUTE (world coordinates). Fixed per
 *            property: the note's per-value mode byte is not built (see the PR).
 *   steps    the nudge sizes the Data Handler offers, in the unit (at most four, multiples of grid).
 *   default  what placement writes and [Reset] puts back.
 *
 * Generated into the schema as `v3:{min:[I;..],max:[I;..],amin:[I;..],amax:[I;..],unit,grid,
 * frame,steps:[I;..],labels:[..],def:{x,y,z},unit_label,frame_label}`; the Data Handler's editor
 * (tools/datahandler/props/DataHandlerVec3.kt) reads it with `data get`, no macro.
 */
data class Vec3i(val x: Int, val y: Int, val z: Int) {
	constructor(all: Int) : this(all, all, all)

	operator fun get(i: Int) = when (i) { 0 -> x; 1 -> y; else -> z }
	fun snbt() = "{x:$x,y:$y,z:$z}"
	fun intArray() = "[I;$x,$y,$z]"
	fun all(p: (Int) -> Boolean) = p(x) && p(y) && p(z)
}

enum class Vec3Frame(val code: Int, val label: String) {
	LOCAL(0, "on the block's face"),
	RELATIVE(1, "from the block"),
	ABSOLUTE(2, "world position"),
}

class Vec3Spec(
	val min: Vec3i = Vec3i(-16),
	val max: Vec3i = Vec3i(16),
	val adminMin: Vec3i? = null,
	val adminMax: Vec3i? = null,
	val unit: Int = 1,
	val grid: Int = 1,
	val frame: Vec3Frame = Vec3Frame.RELATIVE,
	val steps: List<Int> = listOf(1, 4, 16),
	val default: Vec3i = Vec3i(0),
) {
	init {
		require(unit in listOf(1, 2, 4, 8, 16)) { "vec3 unit must be 1, 2, 4, 8 or 16" }
		require(grid >= 1) { "vec3 grid must be at least 1" }
		require(steps.size in 1..MAX_STEPS && steps.all { it > 0 && it % grid == 0 }) { "vec3 steps: 1 to $MAX_STEPS positive multiples of the grid" }
		for (i in 0..2) {
			require(min[i] <= max[i]) { "vec3 min must not be above max" }
			require((adminMin?.get(i) ?: min[i]) <= min[i] && (adminMax?.get(i) ?: max[i]) >= max[i]) { "vec3 admin range must contain the players' range" }
			require(frame == Vec3Frame.ABSOLUTE || default[i] in min[i]..max[i]) { "vec3 default must be in range" }
		}
	}

	/** The technical cap: an admin's range, or the players' when none is declared. */
	val capMin get() = adminMin ?: min
	val capMax get() = adminMax ?: max

	/** [n] steps of the unit as a reader sees it: "1/16", "1/4", "1", "4". */
	fun label(n: Int): String {
		fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
		val g = gcd(n, unit)
		return if (unit / g == 1) "${n / g}" else "${n / g}/${unit / g}"
	}

	val unitLabel get() = if (unit == 1) "blocks" else "1/$unit block"

	/** The schema fields, `v3:{...}`, appended to the property's registry entry. */
	fun snbtFields(): String = buildList {
		add("min:${min.intArray()}")
		add("max:${max.intArray()}")
		adminMin?.let { add("amin:${it.intArray()}") }
		adminMax?.let { add("amax:${it.intArray()}") }
		add("unit:$unit")
		add("grid:$grid")
		add("frame:${frame.code}")
		add("steps:[I;${steps.joinToString(",")}]")
		add("labels:[${steps.joinToString(",") { "\"${label(it)}\"" }}]")
		add("def:${default.snbt()}")
		add("unit_label:\"$unitLabel\"")
		add("frame_label:\"${frame.label}\"")
	}.joinToString(",", "v3:{", "}")

	companion object {
		const val MAX_STEPS = 4
	}
}

/** A vec3 property: see [Vec3Spec]. Its default is [Vec3Spec.default]. */
fun PropertiesBuilder.vec3(
	name: String, spec: Vec3Spec,
	readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null,
) {
	require(specs.none { it.name == name }) { "property $name declared twice" }
	specs += PropSpec(name, PropType.VEC3, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles, default = spec.default.snbt(), vec3 = spec)
}

/**
 * Use time, as the block's marker: `data.properties.<name>` into [into] (a storage path,
 * `storage ra:temp bd.pos`) as `{x:<int>,y:<int>,z:<int>}`, each part copied through a score and
 * clamped to the technical cap ([Vec3Spec.capMin]..[Vec3Spec.capMax]; for an ABSOLUTE frame the
 * value is a world coordinate and is not clamped here). A missing property, or one without all
 * three parts x, y and z, reads as the default.
 * Whatever the property holds (a string from the Creative Data Handler, a list), the result is
 * three integers: safe to paste with `$(x) $(y) $(z)`.
 */
fun Function.readVec3(name: String, spec: Vec3Spec, into: String) {
	val t = Obj.TEMP
	val axes = listOf("x", "y", "z")
	val p = "data.properties.$name"
	raw("data modify $into set value ${spec.default.snbt()}")
	// Only a value with all three parts counts; anything else (missing, a string, a list) is the default.
	raw("execute store success score #v3r.ok $t if data entity @s $p.x if data entity @s $p.y if data entity @s $p.z")
	for ((i, a) in axes.withIndex()) {
		val s = "#v3r.$a"
		raw("scoreboard players set $s $t ${spec.default[i]}")
		raw("execute if score #v3r.ok $t matches 1 store result score $s $t run data get entity @s $p.$a")
		if (spec.frame != Vec3Frame.ABSOLUTE) {
			raw("execute if score $s $t matches ..${spec.capMin[i] - 1} run scoreboard players set $s $t ${spec.capMin[i]}")
			raw("execute if score $s $t matches ${spec.capMax[i] + 1}.. run scoreboard players set $s $t ${spec.capMax[i]}")
		}
		raw("execute store result $into.$a int 1 run scoreboard players get $s $t")
	}
}
