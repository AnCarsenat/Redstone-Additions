package ra.modules.automation.fakeplayer

import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.functions.Function
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Ra
import ra.core.block.readProperty
import ra.core.define
import ra.core.raw
import ra.modules.automation.fakeplayer.FakePlayer.T
import ra.modules.automation.fakeplayer.FakePlayerRun.S
import ra.modules.automation.fakeplayer.FakePlayerRun.SPACE
import ra.modules.automation.fakeplayer.FakePlayerRun.state

/**
 * Natural spawning around a [FakePlayer] with no spawner beside it: the Overworld's hostile mobs,
 * in the dark, by the vanilla rules, never faster than vanilla. As the marker, at the block.
 *
 * WHEN. Not with `spawn_monsters` off, not outside the Overworld, and not while a player (not a
 * spectator) is within 128 blocks: vanilla is spawning around that player already, or, within
 * 24, would spawn nothing here. Not while `max_mobs` hostile mobs are in the cube it spawns in.
 *
 * WHERE. [ATTEMPTS] random blocks a run in the cube of `radius` around it. A spot is good when the
 * mob's feet and head are in free space ([SPACE]), the block under it has a full top it can stand
 * on (not glass, leaves, ice, bedrock, barriers, magma, a bottom slab or stair, a fence, a carpet,
 * a plant...: [FLOOR_DENIED]) and the light there is 0. Vanilla asks for block light 0 too, and
 * sometimes takes a sky light up to 7; asking for 0 overall only ever spawns less.
 *
 * WHAT. The mob is drawn from the biome's vanilla monster list, by the same weights (the lists
 * below, taken from the 26.3 biome files). Entries that cannot spawn in the dark the way the Fake
 * Player looks (slimes need slime chunks or swamps, husks, strays and parched need open sky,
 * zombie horses the surface) keep their weight and spawn nothing when drawn, so the others come
 * no more often than in vanilla. Biomes not listed use the plains list (so no swamp slimes, no
 * drowned in caves or rivers, no bogged, whatever those biomes add); the Mushroom Fields and
 * the Deep Dark spawn nothing. One mob a good spot, where vanilla spawns a pack of up to four.
 */
internal object FakePlayerNatural {
	/** Spawn attempts per run (once a second). */
	const val ATTEMPTS = 4

	val FLOOR_DENIED = "#${Ns.AUTOMATION.id}:fake_player_floor_denied"

	/** One monster entry: [id] null spawns nothing. [h] blocks of headroom, [wide] for a spider. */
	class Entry(val weight: Int, val id: String?, val h: Int = 2, val wide: Boolean = false)

	private val PLAINS = listOf(
		Entry(100, "minecraft:spider", h = 1, wide = true), Entry(90, "minecraft:zombie"),
		Entry(5, "minecraft:zombie_villager"), Entry(5, null), Entry(100, "minecraft:skeleton"),
		Entry(100, "minecraft:creeper"), Entry(100, null), Entry(10, "minecraft:enderman", h = 3),
		Entry(5, "minecraft:witch"),
	)
	private val DESERT = listOf(
		Entry(100, "minecraft:spider", h = 1, wide = true), Entry(19, "minecraft:zombie"),
		Entry(1, "minecraft:zombie_villager"), Entry(50, "minecraft:skeleton"), Entry(100, "minecraft:creeper"),
		Entry(100, null), Entry(10, "minecraft:enderman", h = 3), Entry(5, "minecraft:witch"),
		Entry(80, null), Entry(50, null),
	)
	private val SNOWY = listOf(
		Entry(100, "minecraft:spider", h = 1, wide = true), Entry(90, "minecraft:zombie"),
		Entry(5, "minecraft:zombie_villager"), Entry(5, null), Entry(20, "minecraft:skeleton"),
		Entry(100, "minecraft:creeper"), Entry(100, null), Entry(10, "minecraft:enderman", h = 3),
		Entry(5, "minecraft:witch"), Entry(80, null),
	)

	/** Biomes with their own list, and the biomes where no monster spawns. */
	private val tables = listOf(
		Triple("desert", listOf("minecraft:desert"), DESERT),
		Triple("snowy", listOf("minecraft:snowy_plains", "minecraft:ice_spikes"), SNOWY),
		Triple("plains", emptyList(), PLAINS),
	)
	private val NONE = listOf("minecraft:mushroom_fields", "minecraft:deep_dark")

	private fun fn(path: String) = FakePlayer.block.fn(path)

	val run = fn("natural/run")
	private val attempts = fn("natural/attempts")
	private val attempt = fn("natural/attempt")
	private val spot = fn("natural/spot")
	private val pick = tables.associate { (name, _, _) -> name to fn("natural/pick_$name") }

	fun Ra.registerFakePlayerNatural() {
		pack.blockTag("fake_player_floor_denied", Ns.AUTOMATION.id) {
			// No full top face (the Doorbell's list: passable blocks, slabs, stairs, fences...),
			// plus the full blocks vanilla keeps hostile mobs off.
			add("#ra_audio:not_sturdy")
			for (t in listOf("impermeable", "leaves")) add("#minecraft:$t")
			for (b in listOf("ice", "packed_ice", "blue_ice", "frosted_ice", "bedrock", "barrier", "magma_block", "light", "water", "lava", "powder_snow"))
				add("minecraft:$b")
		}

		pack.define(run) {
			raw("execute store result score #fp.g $T run gamerule spawn_monsters")
			raw("execute if score #fp.g $T matches 0 run return run ${state("monster spawning is off (spawn_monsters)")}")
			raw("execute unless dimension minecraft:overworld run return run ${state("natural spawning: Overworld only")}")
			raw("execute if entity @a[gamemode=!spectator,distance=..128,limit=1] run return run ${state("a player is near: vanilla spawning runs")}")
			readCappedProperty("radius", default = FakePlayer.DEFAULT_RADIUS, min = 1, adminMax = FakePlayer.ADMIN_MAX_RADIUS)
			raw("execute store result storage $S n.r int 1 run scoreboard players get #prop $T")
			raw("scoreboard players operation #fp.d $T = #prop $T")
			raw("scoreboard players operation #fp.d $T += #prop $T")
			raw("execute store result storage $S n.d int 1 run scoreboard players get #fp.d $T")
			readProperty("max_mobs", default = FakePlayer.DEFAULT_MAX_MOBS, min = 1)
			raw("execute if score #prop $T matches ${FakePlayer.MAX_MOBS + 1}.. run scoreboard players set #prop $T ${FakePlayer.MAX_MOBS}")
			raw("scoreboard players operation #fp.cap $T = #prop $T")
			raw("scoreboard players set #fp.i $T 0")
			raw("function ${attempts.asId()} with storage $S n")
			raw("data remove storage $S n")
			raw("execute if score #fp.made $T matches 1.. run function ${FakePlayerRun.counted.asId()}")
			raw("execute if score #fp.made $T matches 1.. run return run ${state("spawning")}")
			raw("execute if score #fp.cap $T matches -1 run return run ${state("mob cap reached")}")
			raw("execute if score #fp.noeu $T matches 1 run return run ${state("not enough EU")}")
			raw(state("looking for dark, free spots"))
		}

		// {r, d}: the radius, a number read from the block and capped, and twice it. One attempt, then the next.
		pack.define(attempts) {
			raw("execute if score #fp.i $T matches $ATTEMPTS.. run return 0")
			raw("scoreboard players add #fp.i $T 1")
			raw("execute if score #fp.noeu $T matches 1 run return 0")
			// The same cube the spawns land in: the blocks -r..r around it.
			raw("\$execute align xyz positioned ~-\$(r) ~-\$(r) ~-\$(r) store result score #fp.n $T if entity @e[type=#ra_interactive:spawn_blocked,dx=\$(d),dy=\$(d),dz=\$(d)]")
			raw("execute if score #fp.n $T >= #fp.cap $T run return run scoreboard players set #fp.cap $T -1")
			for (a in listOf("x", "y", "z")) raw("\$execute store result storage $S n.$a int 1 run random value -\$(r)..\$(r)")
			raw("function ${attempt.asId()} with storage $S n")
			raw("function ${attempts.asId()} with storage $S n")
		}

		// {x, y, z}: random offsets this pack drew.
		pack.define(attempt) {
			raw("\$execute positioned ~\$(x) ~\$(y) ~\$(z) align xyz run function ${spot.asId()}")
		}

		// At the corner of the block the mob would stand in. As the marker.
		pack.define(spot) {
			raw("execute unless block ~ ~ ~ $SPACE run return 0")
			raw("execute unless block ~ ~1 ~ $SPACE run return 0")
			raw("execute if block ~ ~-1 ~ $FLOOR_DENIED run return 0")
			raw("execute if block ~ ~-1 ~ $SPACE run return 0")
			raw("execute positioned ~0.5 ~ ~0.5 unless predicate {type:\"minecraft:location_check\",predicate:{light:{light:{max:0}}}} run return 0")
			for (b in NONE) raw("execute if biome ~ ~ ~ $b run return 0")
			for ((name, biomes, _) in tables) {
				if (biomes.isEmpty()) raw("return run function ${pick.getValue(name).asId()}")
				else for (b in biomes) raw("execute if biome ~ ~ ~ $b run return run function ${pick.getValue(name).asId()}")
			}
		}

		for ((name, _, table) in tables) pack.define(pick.getValue(name)) { pick(table) }
	}

	/** A weighted draw from [table], then the mob's own space check and the spawn. */
	private fun Function.pick(table: List<Entry>) {
		raw("execute store result score #fp.roll $T run random value 1..${table.sumOf { it.weight }}")
		var from = 1
		for (e in table) {
			val to = from + e.weight - 1
			val range = "$from..$to"
			from = to + 1
			if (e.id == null) {
				raw("execute if score #fp.roll $T matches $range run return 0")
				continue
			}
			raw("execute if score #fp.roll $T matches $range run scoreboard players set #fp.h $T ${e.h}")
			raw("execute if score #fp.roll $T matches $range run scoreboard players set #fp.w $T ${if (e.wide) 1 else 0}")
			raw("execute if score #fp.roll $T matches $range unless function ${FakePlayerRun.space.asId()} run return 0")
			raw("execute if score #fp.roll $T matches $range run return run function ${FakePlayerRun.spawnHere.asId()} {id:\"${e.id}\"}")
		}
	}
}
