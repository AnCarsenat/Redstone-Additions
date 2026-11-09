package ra.migrations

import io.github.ayfri.kore.commands.function
import ra.api.ModulesApi
import ra.core.Ns
import ra.core.Obj
import ra.core.PackPart
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.migrations.DataVersion.CURRENT
import ra.migrations.DataVersion.OBJ

/**
 * World and block migrations (`ra_migrations`). EVERY one-time fix for data an older version left
 * behind lives here, and nowhere else: a block's tick, placement or menu never checks whether its
 * marker is from an old version. See docs/contributing-guidelines.md.
 *
 * TWO VERSIONS ARE RECORDED
 * - The world's, in `storage ra:migrations version`, as major*10000+minor*100+patch (6.0.1 is
 *   60001; see [DataVersion]). `ra_migrations:run`, called by `ra:load`, runs each step's [world]
 *   part only when that version is below the step's target, then writes the current version. A
 *   world with no version (anything older than 6.0.1, when it started being recorded) runs every
 *   world part once.
 * - Each custom block marker's, in its `ra.ver` score, plus a tag `ra.ver.<current>`. Placement
 *   stamps both ([DataVersion.stampNew]). `ra_migrations:entities`, run at the top of every
 *   `ra:tick`, finds the markers without the current tag -- never stamped, or stamped by an older
 *   version -- in ONE selector, and runs `ra_migrations:entity` as each: the entity parts of every
 *   step whose target is above the marker's score, then the stamp. A marker in a chunk that was
 *   unloaded at load is caught the first tick it is loaded, before any block tick sees it. When
 *   nothing is outdated the whole pass is that one selector, finding nothing.
 *
 * ADDING A STEP
 * Declare it with [migration] in a file named for the step (`Step6_0_1To6_0_2.kt`), add it to
 * [steps] below, oldest first:
 * ```
 * internal val m6_0_1 = migration("6.0.1", "6.0.2") {
 *     world { raw("data remove storage ra:foo old_key") }          // once per world
 *     entity("liquid_pump") { raw("data remove entity @s data.properties.old") }   // once per pump
 *     entity("teleport_anchor", TeleportAnchor.repairTargets)       // a function, as each anchor
 * }
 * ```
 * Adding a part to the step of the version being released (an entity fix for 6.0.1 while 6.0.1 is
 * in development) is one line in that step.
 *
 * Every part must be idempotent: unversioned worlds and markers run all of them, including fixes
 * their old block ticks may already have applied. See [Migration] for the rules.
 *
 * GENERATED FUNCTIONS
 * - `ra_migrations:<from>-to-<to>`: a step's world part.
 * - `ra_migrations:<from>-to-<to>/<block type>` (or `/all_blocks`): its entity parts, as the marker.
 * - `ra_migrations:<from>-to-<to>/entity`: dispatch of those by marker tag.
 * - `ra_migrations:run`, `ra_migrations:entities`, `ra_migrations:entity`: see above.
 *
 * THE STEP NAMES ARE IDENTIFIERS, not the pack version: a find-and-replace that bumps the version
 * across the repo must not touch them, or the chain silently renames itself to nonsense like
 * `5.1.9-to-5.1.9`. The name is `-to-` rather than `->` because a resource location path may only
 * contain `[a-z0-9_.-/]`; a file with `>` in its name is skipped by the datapack loader.
 */
object MigrationsPart : PackPart {
	/** Every step, oldest first. */
	private val steps get() = listOf(m5_1_6, m5_1_7, m5_1_8, m5_1_9, m5_1_11, m5_1_15, m6_0_0, m6_0_1)

	private val entity = ref(Ns.MIGRATIONS, "entity")
	private val untag = ref(Ns.MIGRATIONS, "entity_untag")

	/** Scratch fake player for the world's version. */
	private const val WORLD_VER = "#mg.ver"

	override fun Ra.register() {
		val steps = steps
		steps.zipWithNext().forEach { (a, b) ->
			check(a.target < b.target) { "Migrations out of order: ${a.id} must come after ${b.id}" }
		}
		check(steps.last().target <= CURRENT) {
			"Migration ${steps.last().id} targets a version newer than the pack (${ra.RedstoneAdditions.VERSION})"
		}

		// Each step's parts.
		val entitySteps = mutableListOf<Migration>()
		for (step in steps) {
			step.worldBody?.let { body -> pack.define(ref(Ns.MIGRATIONS, step.id), body) }
			if (step.entityBodies.isEmpty()) continue
			entitySteps += step
			val dispatch = step.entityBodies.map { (tag, bodies) ->
				val name = if (tag == Tag.CUSTOM_BLOCK) "all_blocks" else tag.removePrefix("${Tag.CUSTOM_BLOCK}.")
				val fn = pack.define(ref(Ns.MIGRATIONS, "${step.id}/$name")) { bodies.forEach { it() } }
				tag to fn
			}
			pack.define(ref(Ns.MIGRATIONS, "${step.id}/entity")) {
				for ((tag, fn) in dispatch) {
					if (tag == Tag.CUSTOM_BLOCK) function(fn)
					else raw("execute if entity @s[tag=$tag] run function ${fn.asId()}")
				}
			}
		}

		// ra:load: the world's steps, each only when the world is older than its target.
		pack.define(ModulesApi.Migrations.run) {
			raw("scoreboard objectives add $OBJ dummy")
			raw("scoreboard players set $WORLD_VER ${Obj.TEMP} 0")
			raw("execute if data storage ra:migrations version store result score $WORLD_VER ${Obj.TEMP} run data get storage ra:migrations version")
			for (step in steps) if (step.worldBody != null)
				raw("execute if score $WORLD_VER ${Obj.TEMP} matches ..${step.target - 1} run function ${Ns.MIGRATIONS.id}:${step.id}")
			raw("data modify storage ra:migrations version set value $CURRENT")
		}

		// ra:tick: every custom block marker not in the current shape. One selector when there
		// are none.
		pack.define(ModulesApi.Migrations.entities) {
			raw("execute as @e[type=minecraft:marker,tag=${Tag.CUSTOM_BLOCK},tag=!${DataVersion.tag()}] run function ${entity.asId()}")
		}

		// As one outdated marker: the steps above its version, oldest first, then the stamp.
		pack.define(entity) {
			raw("execute unless score @s $OBJ matches -2147483648.. run scoreboard players set @s $OBJ 0")
			for (step in entitySteps)
				raw("execute if score @s $OBJ matches ..${step.target - 1} run function ${Ns.MIGRATIONS.id}:${step.id}/entity")
			// The previous version's tag, named by the score it was stamped with.
			raw("execute if score @s $OBJ matches 1.. store result storage ra:migrations untag.v int 1 run scoreboard players get @s $OBJ")
			raw("execute if score @s $OBJ matches 1.. run function ${untag.asId()} with storage ra:migrations untag")
			raw("data remove storage ra:migrations untag")
			raw("scoreboard players set @s $OBJ $CURRENT")
			raw("tag @s add ${DataVersion.tag()}")
		}
		pack.define(untag) { raw("\$tag @s remove ra.ver.\$(v)") }
	}
}
