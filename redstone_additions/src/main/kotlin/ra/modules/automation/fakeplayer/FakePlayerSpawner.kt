package ra.modules.automation.fakeplayer

import ra.api.RaLib
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.raw
import ra.modules.automation.AutomationClock
import ra.modules.automation.fakeplayer.FakePlayer.T
import ra.modules.automation.fakeplayer.FakePlayerRun.S
import ra.modules.automation.fakeplayer.FakePlayerRun.state

/**
 * A vanilla mob spawner touching a [FakePlayer], run the way the spawner runs itself when a player
 * is near. As the marker, positioned at the spawner.
 *
 * WHEN. Not with `spawner_blocks_work` off, and not while a player (not a spectator) is within the
 * spawner's own `RequiredPlayerRange` (16 unless changed): then the spawner is running by itself,
 * and both would double its rate.
 *
 * THE SPAWNER'S OWN NUMBERS. The countdown starts from the spawner's `Delay` and counts down by the
 * clock's 20 ticks; at 0 a batch of `SpawnCount` tries is made, each at a random block within
 * `SpawnRange` across and one up or down, the next countdown drawn between `MinSpawnDelay` and
 * `MaxSpawnDelay`. A try stops the batch when `MaxNearbyEntities` mobs of that type are already in
 * the spawner's box. A spot is good when the mob fits in free space and the light there is 11 or
 * less, as for a vanilla spawner; a spawner does not ask for a floor. The mob is the spawner's
 * `SpawnData` entity: one with no data besides its id is summoned as `/summon` would (random
 * equipment, babies, jockeys), one with data gets exactly that data. `SpawnPotentials` is not
 * stepped through: the spawner keeps showing and spawning its current mob.
 *
 * WHICH MOBS. Only the hostile mobs of [ALLOWED], each with the room it needs: what vanilla's
 * dungeons, mineshafts, fortresses and strongholds hold, and the other common spawner mobs.
 * Slimes and magma cubes (a size of their own), animals and trial spawners are not run.
 */
internal object FakePlayerSpawner {
	/** Mob id -> blocks of headroom it needs, and 1 when it is wider than a block (the spider). */
	private val ALLOWED = listOf(
		"zombie" to 2, "husk" to 2, "zombie_villager" to 2, "drowned" to 2, "skeleton" to 2, "stray" to 2,
		"bogged" to 2, "wither_skeleton" to 3, "spider" to 1, "cave_spider" to 1, "blaze" to 2,
		"silverfish" to 1, "creeper" to 2, "witch" to 2, "enderman" to 3, "zombified_piglin" to 2,
	)
	private val WIDE = setOf("spider")

	private const val NEW = "ra.auto.fp_new"

	private fun fn(path: String) = FakePlayer.block.fn(path)

	val run = fn("spawner/run")
	val load = fn("spawner/load")
	private val near = fn("spawner/near")
	private val allow = fn("spawner/allow")
	private val attempts = fn("spawner/attempts")
	private val attempt = fn("spawner/attempt")
	private val spot = fn("spawner/spot")
	private val spawnFull = fn("spawner/spawn_full")
	private val markNew = fn("spawner/mark_new")
	private val nextDelay = fn("spawner/next_delay")

	fun Ra.registerFakePlayerSpawner() {
		pack.define(load) {
			val map = ALLOWED.joinToString(",", "{", "}") { (id, h) -> "\"minecraft:$id\":{h:$h,w:${if (id in WIDE) 1 else 0}}" }
			raw("data modify storage $S allow set value $map")
		}

		pack.define(run) {
			raw("execute store result score #fp.g $T run gamerule spawner_blocks_work")
			raw("execute if score #fp.g $T matches 0 run return run ${state("spawners are off (spawner_blocks_work)")}")
			raw("data remove storage $S s")
			raw("scoreboard players set #fp.p $T 16")
			raw("execute if data block ~ ~ ~ RequiredPlayerRange store result score #fp.p $T run data get block ~ ~ ~ RequiredPlayerRange")
			raw("execute if score #fp.p $T matches ..0 run scoreboard players set #fp.p $T 16")
			raw("execute store result storage $S s.p int 1 run scoreboard players get #fp.p $T")
			raw("execute store result score #fp.near $T run function ${near.asId()} with storage $S s")
			raw("execute if score #fp.near $T matches 1 run return run ${state("a player is near the spawner: it runs by itself")}")

			// The countdown, from the spawner's own Delay the first time.
			raw("execute unless data entity @s data.data.delay store result entity @s data.data.delay int 1 run data get block ~ ~ ~ Delay")
			raw("execute store result score #fp.t $T run data get entity @s data.data.delay")
			raw("scoreboard players remove #fp.t $T ${AutomationClock.PERIOD}")
			raw("execute store result entity @s data.data.delay int 1 run scoreboard players get #fp.t $T")
			raw("execute if score #fp.t $T matches 1.. run return run ${state("running a spawner: waiting for its delay")}")

			raw("data modify storage $S s.e set from block ~ ~ ~ SpawnData.entity")
			raw("execute unless data storage $S s.e.id run data modify entity @s data.data.delay set value ${AutomationClock.PERIOD}")
			raw("execute unless data storage $S s.e.id run return run ${state("the spawner is empty")}")
			// The id is block data: a format check before it goes near a command, then the table.
			raw("data modify storage ra:safe s set from storage $S s.e.id")
			raw("data modify storage ra:safe f set value ${TextFormat.ID.code}")
			raw("execute store result score #fp.ok $T run function ${RaLib.Safe.check.asId()}")
			raw("data remove storage ra:safe s")
			raw("data remove storage ra:safe f")
			raw("execute if score #fp.ok $T matches 1 run data modify storage $S s.id set from storage $S s.e.id")
			raw("scoreboard players set #fp.ok $T 0")
			raw("execute if data storage $S s.id store result score #fp.ok $T run function ${allow.asId()} with storage $S s")
			raw("execute unless score #fp.ok $T matches 1 run data modify entity @s data.data.delay set value 200")
			raw("execute unless score #fp.ok $T matches 1 run return run ${state("spawner mob not supported")}")
			raw("execute store result score #fp.h $T run data get storage $S s.a.h")
			raw("execute store result score #fp.w $T run data get storage $S s.a.w")

			// Only an id: summoned as /summon would. More data: that data, tagged so it can be found.
			raw("execute store result score #fp.k $T run data get storage $S s.e")
			raw("data modify storage $S s.e2 set from storage $S s.e")
			for (k in listOf("id", "Pos", "UUID", "Motion")) raw("data remove storage $S s.e2.$k")
			raw("execute unless data storage $S s.e2.Tags run data modify storage $S s.e2.Tags set value []")
			raw("data modify storage $S s.e2.Tags append value \"$NEW\"")

			fun num(nbt: String, default: Int, key: String) {
				raw("scoreboard players set #fp.$key $T $default")
				raw("execute if data block ~ ~ ~ $nbt store result score #fp.$key $T run data get block ~ ~ ~ $nbt")
			}
			num("SpawnCount", 4, "cnt")
			num("SpawnRange", 4, "rng")
			num("MaxNearbyEntities", 6, "max")
			num("MinSpawnDelay", 200, "min")
			num("MaxSpawnDelay", 800, "mx")
			raw("execute if score #fp.rng $T matches ..0 run scoreboard players set #fp.rng $T 1")
			raw("execute if score #fp.rng $T matches 17.. run scoreboard players set #fp.rng $T 16")
			raw("execute if score #fp.cnt $T matches 17.. run scoreboard players set #fp.cnt $T 16")
			raw("execute store result storage $S s.r int 1 run scoreboard players get #fp.rng $T")
			raw("scoreboard players operation #fp.d $T = #fp.rng $T")
			raw("scoreboard players operation #fp.d $T += #fp.rng $T")
			raw("execute store result storage $S s.d int 1 run scoreboard players get #fp.d $T")

			raw("scoreboard players set #fp.i $T 0")
			raw("scoreboard players set #fp.full $T 0")
			raw("function ${attempts.asId()} with storage $S s")
			raw("data remove storage $S s")

			raw("execute if score #fp.made $T matches 1.. run function ${FakePlayerRun.counted.asId()}")
			raw("execute if score #fp.made $T matches 1.. run function ${nextDelay.asId()}")
			raw("execute if score #fp.made $T matches 1.. run return run ${state("running a spawner: spawning")}")
			raw("execute if score #fp.full $T matches 1 run function ${nextDelay.asId()}")
			raw("execute if score #fp.full $T matches 1 run return run ${state("running a spawner: enough mobs around it")}")
			raw("data modify entity @s data.data.delay set value ${AutomationClock.PERIOD}")
			raw("execute if score #fp.noeu $T matches 1 run return run ${state("not enough EU")}")
			raw(state("running a spawner: no dark, free spot around it"))
		}

		// {p}: the spawner's player range, a number.
		pack.define(near) {
			raw("\$execute if entity @a[gamemode=!spectator,distance=..\$(p),limit=1] run return 1")
			raw("return 0")
		}

		// {id}: an id that passed the ID format check.
		pack.define(allow) {
			raw("\$execute unless data storage $S allow.\"\$(id)\" run return 0")
			raw("\$data modify storage $S s.a set from storage $S allow.\"\$(id)\"")
			raw("return 1")
		}

		// {id, r, d, ...}: id from the table, r and d numbers. One try, then the next.
		pack.define(attempts) {
			raw("execute if score #fp.i $T >= #fp.cnt $T run return 0")
			raw("scoreboard players add #fp.i $T 1")
			raw("execute if score #fp.noeu $T matches 1 run return 0")
			raw("\$execute align xyz positioned ~-\$(r) ~-\$(r) ~-\$(r) store result score #fp.n $T if entity @e[type=\$(id),dx=\$(d),dy=\$(d),dz=\$(d)]")
			raw("execute if score #fp.n $T >= #fp.max $T run return run scoreboard players set #fp.full $T 1")
			raw("\$execute store result storage $S s.x int 1 run random value -\$(r)..\$(r)")
			raw("execute store result storage $S s.y int 1 run random value -1..1")
			raw("\$execute store result storage $S s.z int 1 run random value -\$(r)..\$(r)")
			raw("function ${attempt.asId()} with storage $S s")
			raw("function ${attempts.asId()} with storage $S s")
		}

		// {x, y, z}: offsets this pack drew.
		pack.define(attempt) {
			raw("\$execute positioned ~\$(x) ~\$(y) ~\$(z) align xyz run function ${spot.asId()}")
		}

		// At the corner of the block the mob would stand in. As the marker.
		pack.define(spot) {
			raw("execute unless function ${FakePlayerRun.space.asId()} run return 0")
			raw("execute positioned ~0.5 ~ ~0.5 unless predicate {type:\"minecraft:location_check\",predicate:{light:{light:{max:11}}}} run return 0")
			raw("execute if score #fp.k $T matches ..1 run return run function ${FakePlayerRun.spawnHere.asId()} with storage $S s")
			raw("function ${spawnFull.asId()} with storage $S s")
		}

		// {id, e2}: the spawner's mob and its data (block data, written as SNBT by the macro).
		pack.define(spawnFull) {
			raw("execute store result score #fp.paid $T run function ${FakePlayerRun.pay.asId()}")
			raw("execute unless score #fp.paid $T matches 1 run return run scoreboard players set #fp.noeu $T 1")
			raw("\$execute positioned ~0.5 ~ ~0.5 run summon \$(id) ~ ~ ~ \$(e2)")
			raw("execute positioned ~0.5 ~ ~0.5 as @e[tag=$NEW,distance=..2] run function ${markNew.asId()}")
		}
		pack.define(markNew) {
			raw("tag @s remove $NEW")
			raw("function ${FakePlayerRun.mark.asId()}")
		}

		// The next countdown, between the spawner's two delays (as the marker).
		pack.define(nextDelay) {
			raw("execute if score #fp.mx $T <= #fp.min $T run scoreboard players operation #fp.mx $T = #fp.min $T")
			raw("execute if score #fp.mx $T <= #fp.min $T run scoreboard players add #fp.mx $T 1")
			raw("execute store result storage $S dl.min int 1 run scoreboard players get #fp.min $T")
			raw("execute store result storage $S dl.max int 1 run scoreboard players get #fp.mx $T")
			raw("function ${FakePlayer.block.fn("spawner/draw_delay").asId()} with storage $S dl")
			raw("data remove storage $S dl")
		}
		pack.define(FakePlayer.block.fn("spawner/draw_delay")) {
			raw("\$execute store result entity @s data.data.delay int 1 run random value \$(min)..\$(max)")
		}
	}
}
