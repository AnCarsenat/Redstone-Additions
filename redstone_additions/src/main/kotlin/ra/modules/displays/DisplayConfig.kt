package ra.modules.displays

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.PropertiesBuilder
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw

/**
 * THE CONFIGURATION EVERY DISPLAY BLOCK SHARES: block properties, declared and handled once here.
 *
 *   facing     the face the display is drawn on (or floats from): down, up, north, south, west,
 *              east. Placement writes the facing the block was placed with.
 *   direction  how the display is turned on that face; each block says what its values mean.
 *   <colours>  each block's own colour properties ([ColorProp]).
 *   offset_x/y/z  a shift from the default spot, in 1/16 block, -16..16, in the display's own
 *              frame: x to the viewer's right, y up, z out of the face.
 *   scale      size in percent, 25..400 (admins up to [ADMIN_MAX_SCALE]), about the face centre.
 *
 * WHEN IT IS APPLIED. A block's `restyle` function runs [defaults] (a property missing from a
 * block placed by an earlier build gets its default), [turn] (the facing as each display's
 * Rotation), [read] (scale and offsets into scores), then one [defineForms] function per display
 * part. `restyle` runs on placement, from the wrench, and from the module's one-second poll when
 * `data.properties` differs from the copy last drawn (`data.data.style`): a Data Handler edit
 * shows within a second, and nothing is added per tick.
 *
 * THE TRANSFORMATION MATH, in one place ([defineForms]). A part is a box (or a text, an item)
 * with a base translation `t` and scale `s` at 100 %, scaled about an anchor `a` (the face
 * centre): translation = a + offset + k * R(t - a), scale = k * s, left_rotation = R, where R is
 * the part's roll on its face. `R(t - a)` is computed here, in Kotlin, for every variant; the
 * game only multiplies by k and adds the offset, in fixed point (1e-7 block), with scoreboard
 * operations. No macro: nothing a player typed reaches a command.
 */
internal class DisplayStyle(
	val block: CustomBlock,
	/** The facing a block placed without one (or by an earlier build) gets. */
	val defaultFacing: String,
	/** What `direction` may be; the first is the default. */
	val directions: List<String>,
	val colors: List<ColorProp>,
	/** The display selectors [turn] rotates. */
	val displays: List<String>,
	/** Horizontal facings only turn the yaw; up and down keep the displays upright (the hologram). */
	val yawOnly: Boolean = false,
	/** The block is a barrel whose lid follows `facing`. */
	val barrel: Boolean = false,
) {
	val defaults = block.fn("config/defaults")
	val turn = block.fn("config/turn")
	val read = block.fn("config/read")
	private val turnBarrel = block.fn("config/barrel")
	private fun barrelTo(f: String) = block.fn("config/barrel_$f")
	val cycleFacing = block.fn("config/cycle_facing")
	val cycleDirection = block.fn("config/cycle_direction")
	fun cycleColor(c: ColorProp) = block.fn("config/cycle_${c.prop}")

	/** `{facing:"...",direction:"...",...,scale:100}` without braces: what placement writes. */
	fun placementSnbt(): String = (listOf("facing:\"$defaultFacing\"", "direction:\"${directions.first()}\"") +
		colors.map { "${it.prop}:\"${it.default}\"" } +
		AXES.map { "offset_$it:0" } + "scale:100").joinToString(",")

	/** In the block's `properties { }`, after its own. */
	fun PropertiesBuilder.declare() {
		text("facing", default = defaultFacing)
		text("direction", default = directions.first())
		for (c in colors) text(c.prop, default = c.default)
		for (a in AXES) number("offset_$a", min = -MAX_OFFSET, max = MAX_OFFSET, default = 0, goggles = false)
		number("scale", min = MIN_SCALE, max = MAX_SCALE, adminMax = ADMIN_MAX_SCALE, default = 100)
	}

	/** Wrench rows: facing, direction, then each colour. */
	fun wrenchRows(): Array<WrenchCycle> = (listOf(WrenchCycle("Facing", "facing", cycleFacing), WrenchCycle("Direction", "direction", cycleDirection)) +
		colors.filter { it.cycle.isNotEmpty() }.map { WrenchCycle(it.label, it.prop, cycleColor(it)) }).toTypedArray()

	/** [restyle]: the block's redraw, run after every wrench change. */
	fun Ra.registerStyle(restyle: FunctionArgument) {
		pack.define(defaults) {
			// The facing an earlier build kept in data.status, else the default.
			raw("execute unless data entity @s data.properties.facing if data entity @s data.status.facing run data modify entity @s data.properties.facing set from entity @s data.status.facing")
			val all = listOf("facing" to "\"$defaultFacing\"", "direction" to "\"${directions.first()}\"") +
				colors.map { it.prop to "\"${it.default}\"" } + AXES.map { "offset_$it" to "0" } + ("scale" to "100")
			for ((p, v) in all) raw("execute unless data entity @s data.properties.$p run data modify entity @s data.properties.$p set value $v")
		}

		pack.define(turn) {
			for (f in FACINGS) {
				val (yaw, pitch) = rotation(f, yawOnly)
				for (d in displays) raw("""execute if data entity @s data.properties{facing:"$f"} run data modify entity $d Rotation set value [${yaw}f,${pitch}f]""")
			}
			if (barrel) function(turnBarrel)
		}

		if (barrel) {
			pack.define(turnBarrel) {
				for (f in FACINGS) executeRun("""if data entity @s data.properties{facing:"$f"} unless block ~ ~ ~ minecraft:barrel[facing=$f]""", barrelTo(f))
			}
			// A barrel's facing is a block state: setblock replaces the block entity, so its
			// contents are carried over by copy, never pasted.
			for (f in FACINGS) pack.define(barrelTo(f)) {
				raw("data modify storage ra:temp dsp.barrel set value {}")
				raw("data modify storage ra:temp dsp.barrel set from block ~ ~ ~")
				raw("setblock ~ ~ ~ minecraft:barrel[facing=$f] replace")
				raw("execute if data storage ra:temp dsp.barrel.Items run data modify block ~ ~ ~ Items set from storage ra:temp dsp.barrel.Items")
				raw("execute if data storage ra:temp dsp.barrel.CustomName run data modify block ~ ~ ~ CustomName set from storage ra:temp dsp.barrel.CustomName")
			}
		}

		pack.define(read) {
			raw("execute store result score $K $T run data get entity @s data.properties.scale")
			raw("execute unless score $K $T matches $MIN_SCALE.. run scoreboard players set $K $T $MIN_SCALE")
			raw("execute if score $K $T matches ${ADMIN_MAX_SCALE + 1}.. run scoreboard players set $K $T $ADMIN_MAX_SCALE")
			AXES.forEachIndexed { i, a ->
				// 1/16 block is 625000 units of 1e-7.
				raw("execute store result score #dsp.o$i $T run data get entity @s data.properties.offset_$a 625000")
				raw("execute if score #dsp.o$i $T matches ..${-MAX_OFFSET * 625000 - 1} run scoreboard players set #dsp.o$i $T ${-MAX_OFFSET * 625000}")
				raw("execute if score #dsp.o$i $T matches ${MAX_OFFSET * 625000 + 1}.. run scoreboard players set #dsp.o$i $T ${MAX_OFFSET * 625000}")
			}
		}

		pack.define(cycleFacing) { cycleText("facing", FACINGS, "Facing", restyle) }
		pack.define(cycleDirection) { cycleText("direction", directions, "Direction", restyle) }
		for (c in colors) if (c.cycle.isNotEmpty()) pack.define(cycleColor(c)) { cycleText(c.prop, c.cycle, c.label, restyle) }
	}

	/**
	 * Wrench: the next of [values] after the current one (the first after anything else). Read
	 * into a score first, so a value just written is never tested again. As the marker, at it.
	 */
	private fun Function.cycleText(prop: String, values: List<String>, label: String, after: FunctionArgument) {
		val m = "#dsp.c $T"
		raw("scoreboard players set $m -1")
		values.forEachIndexed { i, v -> raw("""execute if data entity @s data.properties{$prop:"$v"} run scoreboard players set $m $i""") }
		for (i in -1 until values.size) {
			val next = values[(i + 1) % values.size]
			raw("""execute if score $m matches $i run data modify entity @s data.properties.$prop set value "$next"""")
			raw("""execute if score $m matches $i run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name} $label: ",color:"gray"},{text:"$next",color:"aqua"}]""")
		}
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		function(after)
	}

	companion object {
		val FACINGS = listOf("down", "up", "north", "south", "west", "east")
		val AXES = listOf("x", "y", "z")
		const val MAX_OFFSET = 16
		const val MIN_SCALE = 25
		const val MAX_SCALE = 400
		const val ADMIN_MAX_SCALE = 1600

		private const val T = Obj.TEMP
		private const val K = "#dsp.k"

		/**
		 * The entity Rotation that turns a display's local +z out of face [f] (+x to the viewer's
		 * right, +y up, or north on the top face). [yawOnly]: up and down stay unturned.
		 */
		fun rotation(f: String, yawOnly: Boolean): Pair<Float, Float> = when (f) {
			"north" -> 180f to 0f
			"west" -> 90f to 0f
			"east" -> -90f to 0f
			"up" -> if (yawOnly) 0f to 0f else 0f to -90f
			"down" -> if (yawOnly) 0f to 0f else 0f to 90f
			else -> 0f to 0f
		}

		/**
		 * `<block>/form/<part>`: [target]'s transformation from [read]'s scores. [variants] keyed
		 * by the value of [prop] (one entry, any key, for a part that does not vary); anything
		 * else draws as the first. [keepLeft]: the part's left_rotation is its own (the
		 * hologram's spin) and is kept. Returns the function, run as the marker after [read].
		 */
		fun Ra.defineForms(block: CustomBlock, part: String, target: String, variants: Map<String, Form>, prop: String = "direction", keepLeft: Boolean = false): FunctionArgument {
			val fn = block.fn("form/$part")
			if (variants.size == 1) {
				pack.define(fn) { form(target, variants.values.first(), keepLeft) }
				return fn
			}
			fun variant(key: String) = block.fn("form/${part}_$key")
			pack.define(fn) {
				for (key in variants.keys) executeRun("""if data entity @s data.properties{$prop:"$key"}""", variant(key))
				val known = variants.keys.joinToString(" ") { """unless data entity @s data.properties{$prop:"$it"}""" }
				executeRun(known, variant(variants.keys.first()))
			}
			for ((key, f) in variants) pack.define(variant(key)) { form(target, f, keepLeft) }
			return fn
		}

		private fun Function.form(target: String, f: Form, keepLeft: Boolean) {
			val q = f.quaternion()
			raw("data modify storage ra:temp dsp.tf set value {left_rotation:[${q.joinToString("f,")}f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0f,0f,0f]}")
			if (keepLeft) raw("data modify storage ra:temp dsp.tf.left_rotation set from entity $target transformation.left_rotation")
			val c = f.rolled(f.t - f.anchor)
			for (i in 0..2) {
				val ci = Math.round(c[i] * 100_000).toInt()
				val ai = Math.round(f.anchor[i] * 10_000_000).toInt()
				if (ci == 0) raw("scoreboard players operation #dsp.v $T = #dsp.o$i $T")
				else {
					raw("scoreboard players set #dsp.v $T $ci")
					raw("scoreboard players operation #dsp.v $T *= $K $T")
					raw("scoreboard players operation #dsp.v $T += #dsp.o$i $T")
				}
				if (ai > 0) raw("scoreboard players add #dsp.v $T $ai")
				if (ai < 0) raw("scoreboard players remove #dsp.v $T ${-ai}")
				raw("execute store result storage ra:temp dsp.tf.translation[$i] float 0.0000001 run scoreboard players get #dsp.v $T")
			}
			for (i in 0..2) {
				if (f.s[i] == 0.0) continue
				raw("execute store result storage ra:temp dsp.tf.scale[$i] float ${num(f.s[i] / 100)} run scoreboard players get $K $T")
			}
			raw("data modify entity $target transformation set from storage ra:temp dsp.tf")
		}

		private fun num(x: Double) = "%.8f".format(java.util.Locale.ROOT, x).trimEnd('0').trimEnd('.')
	}
}

/** A colour property of a display block: its [label] on the wrench, its default, what the wrench cycles through (empty: no row). */
internal class ColorProp(val prop: String, val label: String, val default: String, val cycle: List<String>)

/** A point or size in a display's local frame, in blocks. */
internal data class Vec(val x: Double, val y: Double, val z: Double) {
	operator fun minus(o: Vec) = Vec(x - o.x, y - o.y, z - o.z)
	operator fun get(i: Int) = when (i) { 0 -> x; 1 -> y; else -> z }
}

/**
 * One part's form at 100 %: base translation [t], base scale [s] (0 on an axis something else
 * sets, the gauge's fill length), the point it scales about [anchor], and its [roll] about the
 * face normal (local z), in degrees counter-clockwise as seen from the front.
 */
internal class Form(val t: Vec, val s: Vec, val anchor: Vec, val roll: Int = 0) {
	private val rad get() = Math.toRadians(roll.toDouble())

	fun rolled(v: Vec): Vec {
		val c = Math.cos(rad); val s = Math.sin(rad)
		return Vec(v.x * c - v.y * s, v.x * s + v.y * c, v.z)
	}

	/** [x, y, z, w], rounded so 90-degree turns come out exact. */
	fun quaternion(): List<String> {
		val h = rad / 2
		return listOf(0.0, 0.0, Math.sin(h), Math.cos(h)).map { "%.6f".format(java.util.Locale.ROOT, it).trimEnd('0').trimEnd('.').let { s -> if (s == "-0") "0" else s } }
	}
}
