package ra.migrations

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.RedstoneAdditions
import ra.core.Tag
import ra.core.raw

/**
 * Pack versions as the integers a world and a marker remember them by.
 *
 * `major * 10000 + minor * 100 + patch`: 6.0.1 is 60001, 5.1.15 is 50115. Two digits each for
 * minor and patch, which the pack has never come near; [parse] refuses anything that would not fit
 * rather than let 5.1.100 compare equal to 5.2.0.
 */
object DataVersion {
	/** The score every custom block marker carries: the version its data is in. */
	const val OBJ = "ra.ver"

	/** `6.0.1` -> 60001. */
	fun parse(version: String): Int {
		val m = Regex("""^(\d+)\.(\d+)\.(\d+)$""").find(version.trim())
			?: error("Not a major.minor.patch version, so migrations cannot be ordered against it: '$version'")
		val (major, minor, patch) = m.destructured.toList().map { it.toInt() }
		require(minor < 100 && patch < 100) { "Version $version does not fit major*10000+minor*100+patch" }
		return major * 10000 + minor * 100 + patch
	}

	/** This build's version, from [RedstoneAdditions.VERSION]. */
	val CURRENT: Int by lazy { parse(RedstoneAdditions.VERSION) }

	/**
	 * The tag a marker wears while its data is in [CURRENT]'s shape. It is what makes the
	 * per-tick search for outdated markers ONE selector: `tag=!ra.ver.60001` matches a marker that
	 * was never stamped and one stamped by an older version alike, which no score test can do
	 * (a `scores=` filter never matches an entity that has no score). The score [OBJ] is still
	 * what the steps compare against; the tag is only the fast "nothing to do" answer.
	 */
	fun tag(version: Int = CURRENT) = "ra.ver.$version"

	/**
	 * Stamp [selector] (one freshly summoned marker) as already in the current shape, so the
	 * migration pass never visits it. Every placement path calls this; one that forgets costs
	 * nothing worse than a single run of the (idempotent) entity steps on that marker.
	 */
	fun Function.stampNew(selector: String) {
		raw("scoreboard players set $selector $OBJ $CURRENT")
		raw("tag $selector add ${tag()}")
	}
}

/**
 * One version step: what a world, and each kind of marker in it, needs in order to go from [from]
 * to [to]. Built with [migration]; listed, oldest first, in [MigrationsPart].
 *
 * ```
 * internal val m6_0_0 = migration("6.0.0", "6.0.1") {
 *     world { ... }                                  // once per world, at load
 *     entity("liquid_drain") { ... }                 // as each liquid_drain marker, once
 *     entity("teleport_anchor", TeleportAnchor.repairTargets)
 *     marker(Tag.CUSTOM_BLOCK) { ... }               // as every custom block marker, once
 * }
 * ```
 *
 * WHEN THEY RUN
 * - [world]: from `ra_migrations:run` (in `ra:load`) when the world's stored version
 *   (`storage ra:migrations version`) is below [to], or when the world has none (older than
 *   6.0.1, when the version started being recorded). Then never again for that world.
 * - [entity] / [marker]: as a marker (`@s`, not positioned -- use `at @s` if you need it), from
 *   the migration pass in `ra:tick`, when that marker's `ra.ver` score is below [to] or missing.
 *   This reaches markers in chunks that were not loaded at load time: they are migrated on the
 *   first tick they are loaded, before any block tick sees them.
 *
 * RULES
 * - Idempotent. A world or marker with no recorded version runs EVERY step, including ones whose
 *   change it may already have (the code they replace ran in block ticks, so a 6.0.0 drain may be
 *   converted already). Act only while the old shape is there.
 * - Never destroy a player's setting to make room for the new one: fill in, rename, remove dead
 *   keys.
 * - [from]/[to] are identifiers, not the build version. A repo-wide version bump must not touch
 *   them (see [MigrationsPart]).
 */
class Migration internal constructor(val from: String, val to: String) {
	/** The version a world or marker must be below for this step to run. */
	val target: Int = DataVersion.parse(to)

	/** `ra_migrations:<from>-to-<to>`, also the folder of this step's per-entity functions. */
	val id: String = "$from-to-$to"

	internal var worldBody: (Function.() -> Unit)? = null

	/** Marker tag -> bodies, in declaration order. */
	internal val entityBodies = linkedMapOf<String, MutableList<Function.() -> Unit>>()

	/** Once per world. Becomes `ra_migrations:<from>-to-<to>`. */
	fun world(body: Function.() -> Unit) {
		check(worldBody == null) { "Migration $id declares world { } twice" }
		worldBody = body
	}

	/** As each marker of the custom block [types] (`ra.custom_block.<type>`), once per marker. */
	fun entity(vararg types: String, body: Function.() -> Unit) {
		for (type in types) marker(Tag.customBlock(type), body)
	}

	/** [entity] calling one existing function as the marker: the one-line form. */
	fun entity(type: String, fn: FunctionArgument) = entity(type) { function(fn) }

	/**
	 * As each custom block marker carrying [tag], once per marker. The pass visits custom block
	 * markers only ([Tag.CUSTOM_BLOCK]), so [tag] is that tag (every block) or a narrower one.
	 */
	fun marker(tag: String, body: Function.() -> Unit) {
		require(tag.startsWith(Tag.CUSTOM_BLOCK)) { "Migration $id: the pass only visits custom block markers, so marker($tag) would never run" }
		entityBodies.getOrPut(tag) { mutableListOf() } += body
	}
}

/** Declares the step from [from] to [to]. See [Migration]. */
internal fun migration(from: String, to: String, build: Migration.() -> Unit) = Migration(from, to).apply(build)
