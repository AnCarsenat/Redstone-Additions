package ra.api

import io.github.ayfri.kore.generated.arguments.types.PredicateArgument
import ra.core.Ns
import ra.core.Ra
import ra.core.Tag

/**
 * A multiblock structure described in Kotlin, compiled at build time by ra_lib_multiblock.
 *
 * Offsets are written ONCE, relative to the base block, for a structure facing **north** (in
 * front of the base is -Z). Because the spec is known when the pack is generated, the library
 * rotates it here instead of in mcfunction, and emits for every facing a predicate
 * (`ra_lib_multiblock:structure/<id>/<facing>`) that tests the whole pattern in one
 * `execute if predicate`. No `build/` queue runs on load and no macro walks the block list on
 * assembly or validation.
 *
 * Register one with [Ra.multiblock] from a part's `register()`. Build one with [multiblockSpec].
 *
 * The storage view (`storage ra:multiblock types.<id>`, with `facings.<f>.{blocks,inputs,
 * outputs,controls}`) is still written on load, as a literal, so everything that reads it keeps
 * working.
 */
class MultiblockSpec(
	/** The type: `data.type` on the marker, `ra.multiblock.<id>` tag. `[a-z0-9_]`. */
	val id: String,
	/** Shown by the goggles. */
	val name: String,
	/** The base tier the wrench assembles it from (`copper`, `iron`, ...). */
	val tier: String,
	/** What the wrench tells a player whose structure is incomplete. */
	val hint: String,
	/** Required blocks, north-facing. */
	val blocks: List<Block>,
	val inputs: List<Port>,
	val outputs: List<Port>,
	val controls: List<Port>,
	/** false for a rotationally symmetric structure: one predicate, facing ignored when checking. */
	val rotates: Boolean,
	/** The `data.facing` a non-rotating structure is assembled with. */
	val symmetricFacing: String,
) {
	/** A block offset from the base. */
	data class Pos(val x: Int, val y: Int, val z: Int) {
		/**
		 * The north-authored offset turned to [facing], by quarter turns about the vertical axis:
		 * north (x, y, z), south (-x, y, -z), east (-z, y, x), west (z, y, -x).
		 */
		fun rotated(facing: String): Pos = when (facing) {
			"north" -> this
			"south" -> Pos(-x, y, -z)
			"east" -> Pos(-z, y, x)
			"west" -> Pos(z, y, -x)
			else -> error("unknown facing '$facing'")
		}
	}

	/**
	 * One required block. [match] is a block id (`minecraft:barrel`), a block tag
	 * (`#ra_multiblock:blast_forge_bricks`) or an id with states (`minecraft:furnace[lit=true]`),
	 * the same strings `execute if block` takes, minus NBT.
	 */
	data class Block(val pos: Pos, val match: String)

	/** A named IO position. */
	data class Port(val name: String, val pos: Pos)

	/** The marker tag the library gives this type's markers. */
	val tag: String get() = Tag.multiblock(id)

	/** The facings this spec is checked in: four, or just [symmetricFacing]. */
	val facings: List<String> get() = if (rotates) FACINGS else listOf(symmetricFacing)

	/** [blocks] turned to [facing] (unchanged for a non-rotating spec). */
	fun blocksFacing(facing: String): List<Block> =
		if (rotates) blocks.map { it.copy(pos = it.pos.rotated(facing)) } else blocks

	/** [ports] turned to [facing] (unchanged for a non-rotating spec). */
	fun portsFacing(ports: List<Port>, facing: String): List<Port> =
		if (rotates) ports.map { it.copy(pos = it.pos.rotated(facing)) } else ports

	/**
	 * `ra_lib_multiblock:structure/<id>/<facing>`, true where the whole pattern stands around the
	 * current position. A non-rotating spec has one predicate, `structure/<id>`, for any facing.
	 */
	fun predicate(facing: String = symmetricFacing): PredicateArgument =
		PredicateArgument(if (rotates) "structure/$id/$facing" else "structure/$id", Ns.LIB_MULTIBLOCK.id)

	init {
		require(Regex("[a-z0-9_]+").matches(id)) { "multiblock id '$id' must be [a-z0-9_]" }
		require(blocks.isNotEmpty()) { "multiblock '$id' has no blocks" }
		require(rotates || symmetricFacing in FACINGS + "none") { "multiblock '$id': bad symmetricFacing '$symmetricFacing'" }
		for (b in blocks) require('{' !in b.match) { "multiblock '$id': NBT in match '${b.match}' is not supported by compiled specs" }
		for (list in listOf(inputs, outputs, controls)) {
			val dup = list.groupBy { it.name }.filterValues { it.size > 1 }.keys
			require(dup.isEmpty()) { "multiblock '$id': duplicate port names $dup" }
		}
	}

	companion object {
		/** All facings, in the order assembly tries them. */
		val FACINGS = listOf("north", "south", "east", "west")
	}
}

/** Builder for [MultiblockSpec]. Every offset is north-facing, relative to the base. */
class MultiblockSpecBuilder(private val id: String, private val name: String, private val tier: String) {
	var hint: String = ""
	/** Set false for a rotationally symmetric structure. */
	var rotates: Boolean = true
	/** The facing stored on the marker of a non-rotating structure (`north`, or `none`). */
	var symmetricFacing: String = "north"

	private val blocks = mutableListOf<MultiblockSpec.Block>()
	private val inputs = mutableListOf<MultiblockSpec.Port>()
	private val outputs = mutableListOf<MultiblockSpec.Port>()
	private val controls = mutableListOf<MultiblockSpec.Port>()

	fun block(x: Int, y: Int, z: Int, match: String) {
		blocks += MultiblockSpec.Block(MultiblockSpec.Pos(x, y, z), match)
	}

	/** The same [match] at several offsets. */
	fun blocks(match: String, vararg at: MultiblockSpec.Pos) = at.forEach { block(it.x, it.y, it.z, match) }

	/** Every position of a box (inclusive ranges) not rejected by [except]. */
	fun box(
		xs: IntRange, ys: IntRange, zs: IntRange, match: String,
		except: (MultiblockSpec.Pos) -> Boolean = { false },
	) {
		for (y in ys) for (z in zs) for (x in xs) {
			val p = MultiblockSpec.Pos(x, y, z)
			if (!except(p)) block(x, y, z, match)
		}
	}

	fun input(name: String, x: Int, y: Int, z: Int) { inputs += MultiblockSpec.Port(name, MultiblockSpec.Pos(x, y, z)) }
	fun output(name: String, x: Int, y: Int, z: Int) { outputs += MultiblockSpec.Port(name, MultiblockSpec.Pos(x, y, z)) }
	fun control(name: String, x: Int, y: Int, z: Int) { controls += MultiblockSpec.Port(name, MultiblockSpec.Pos(x, y, z)) }

	fun build() = MultiblockSpec(id, name, tier, hint, blocks.toList(), inputs.toList(), outputs.toList(), controls.toList(), rotates, symmetricFacing)
}

/** `multiblockSpec("rock_metallic_drill", "Rock Metallic Drill", "copper") { block(0, 0, 0, "minecraft:waxed_copper_block"); ... }` */
fun multiblockSpec(id: String, name: String, tier: String, block: MultiblockSpecBuilder.() -> Unit): MultiblockSpec =
	MultiblockSpecBuilder(id, name, tier).apply(block).build()

/** `MultiblockSpec.Pos(x, y, z)`, for [MultiblockSpecBuilder.blocks]. */
fun pos(x: Int, y: Int, z: Int) = MultiblockSpec.Pos(x, y, z)

/**
 * Registers a compiled multiblock. Call it from a part's `register()`: ra_lib_multiblock compiles
 * every registered spec once all parts have registered (predicates, validator, checker, tagger,
 * the try_tier entry and the storage view). Hook only the type's tick (and, if it needs one, its
 * `on_break`); assembly, setup and validation are generated.
 */
fun Ra.multiblock(spec: MultiblockSpec): MultiblockSpec {
	check(!multiblockSpecsCompiled) { "multiblock '${spec.id}' registered after ra_lib_multiblock compiled its specs; register it from register(), not afterAllParts" }
	require(multiblockSpecs.none { it.id == spec.id }) { "multiblock '${spec.id}' registered twice" }
	multiblockSpecs += spec
	return spec
}
