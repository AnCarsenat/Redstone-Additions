package ra.modules.automation.fakeplayer

import io.github.ayfri.kore.features.tags.blockTag
import ra.api.RaLib
import ra.core.Ns
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.readProperty
import ra.core.define
import ra.core.raw
import ra.modules.automation.fakeplayer.FakePlayer.HELD
import ra.modules.automation.fakeplayer.FakePlayer.HOLD
import ra.modules.automation.fakeplayer.FakePlayer.MOB
import ra.modules.automation.fakeplayer.FakePlayer.T
import ra.modules.automation.fakeplayer.FakePlayerNatural.registerFakePlayerNatural
import ra.modules.automation.fakeplayer.FakePlayerSpawner.registerFakePlayerSpawner

/**
 * One run of a [FakePlayer], once a second from the automation clock. As the marker, at the block.
 *
 * ORDER. First it holds its mobs (below). Then nothing at all in peaceful or with `spawn_mobs`
 * off. Then a vanilla spawner touching one of its six faces is run ([FakePlayerSpawner]); without
 * one, natural spawning around it ([FakePlayerNatural]).
 *
 * HOLDING, WITH TAGS ONLY. Every mob a Fake Player spawns is tagged [MOB] and made persistent, and
 * each run every such mob within [HOLD] blocks is tagged [HELD]: as long as a Fake Player is near,
 * it does not despawn, as a mob near a player does not. Every [FakePlayer.SWEEP_RUNS] clock runs
 * the sweep ([sweep]) releases the [MOB]s that were not [HELD] since the last sweep (they wandered
 * off, the block was broken): they lose both tags and the persistence and are ordinary mobs again,
 * despawning by the vanilla rules. Then it clears [HELD] for the next round. No score per mob, so a
 * mob that dies leaves nothing behind. A mob a player names keeps its own persistence.
 *
 * EU. Each mob costs `eu_per_mob`, taken whole from the first wire or battery touching the block
 * whose grid holds it, before the mob is summoned: no EU, no mob. A summon that brings no mob
 * (the game refused it) gives the price back ([refund]).
 */
internal object FakePlayerRun {
	const val S = "ra_automation:fake_player"
	private const val SRC = "ra.auto.fp_src"
	private val SIDES = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~1 ~", "~ ~-1 ~")

	/** Free space a mob may stand in: no collision, no fluid, nothing the game keeps mobs out of. */
	val SPACE = "#${Ns.AUTOMATION.id}:fake_player_space"

	private fun fn(path: String) = FakePlayer.block.fn(path)

	val run = fn("run")
	val sweep = fn("sweep")
	private val release = fn("release")
	private val scanEu = fn("scan_eu")
	private val take = fn("take")
	private val giveBack = fn("give_back")
	private val offer = fn("offer")

	/** As the marker: 1 when `eu_per_mob` was paid, 0 when no grid beside the block holds it. */
	val pay = fn("pay")

	/** As the marker: the `#fp.cost` just paid back into a grid beside the block. */
	val refund = fn("refund")

	/** As the new mob: tagged, persistent, held now. */
	val mark = fn("mark")

	/** At the block the mob stands in: 1 when it is free [SPACE], #fp.h tall and, for #fp.w 1, wide. */
	val space = fn("space")

	/** Macro `{id}`, as the marker, at the block the mob stands in: pay, summon as /summon would. */
	val spawnHere = fn("spawn_here")

	/** As the marker: `data.status.spawned` += #fp.made. */
	val counted = fn("counted")

	fun state(text: String) = "data modify entity @s data.status.state set value \"$text\""

	fun Ra.registerFakePlayerRun() {
		pack.blockTag("fake_player_space", Ns.AUTOMATION.id) {
			for (b in listOf("air", "cave_air", "short_grass", "fern", "dead_bush", "short_dry_grass")) add("minecraft:$b")
			for (t in listOf("small_flowers", "saplings")) add("#minecraft:$t")
		}

		pack.define(run) {
			raw("tag @e[tag=$MOB,tag=!$HELD,distance=..$HOLD] add $HELD")
			raw("scoreboard players set #fp.made $T 0")
			raw("scoreboard players set #fp.noeu $T 0")
			raw("execute store result score #fp.g $T run difficulty")
			raw("execute if score #fp.g $T matches 0 run return run ${state("peaceful: nothing spawns")}")
			raw("execute store result score #fp.g $T run gamerule spawn_mobs")
			raw("execute if score #fp.g $T matches 0 run return run ${state("mob spawning is off (spawn_mobs)")}")
			for (at in SIDES) raw("execute if block $at minecraft:spawner positioned $at run return run function ${FakePlayerSpawner.run.asId()}")
			raw("function ${FakePlayerNatural.run.asId()}")
		}

		registerFakePlayerSweep()

		pack.define(counted) {
			raw("execute store result score #fp.c $T run data get entity @s data.status.spawned")
			raw("scoreboard players operation #fp.c $T += #fp.made $T")
			raw("execute store result entity @s data.status.spawned int 1 run scoreboard players get #fp.c $T")
		}

		pack.define(mark) {
			raw("tag @s add $MOB")
			raw("tag @s add $HELD")
			raw("data modify entity @s PersistenceRequired set value 1b")
			raw("execute store result entity @s Rotation[0] float 1 run random value 0..359")
			raw("scoreboard players add #fp.made $T 1")
			raw("execute at @s run particle minecraft:poof ~ ~0.5 ~ 0.2 0.3 0.2 0.01 6 normal ${particleViewersSel(32)}")
		}

		// #fp.h blocks of free space up, and for a wide mob (#fp.w 1) the eight around the feet.
		pack.define(space) {
			raw("execute unless block ~ ~ ~ $SPACE run return 0")
			raw("execute if score #fp.h $T matches 2.. unless block ~ ~1 ~ $SPACE run return 0")
			raw("execute if score #fp.h $T matches 3.. unless block ~ ~2 ~ $SPACE run return 0")
			for ((x, z) in listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1))
				raw("execute if score #fp.w $T matches 1 unless block ~$x ~ ~$z $SPACE run return 0")
			raw("return 1")
		}

		// {id}: a registry id from this pack's own tables, never text from a player or a block.
		pack.define(spawnHere) {
			raw("execute store result score #fp.paid $T run function ${pay.asId()}")
			raw("execute unless score #fp.paid $T matches 1 run return run scoreboard players set #fp.noeu $T 1")
			raw("scoreboard players operation #fp.before $T = #fp.made $T")
			raw("\$execute positioned ~0.5 ~ ~0.5 summon \$(id) run function ${mark.asId()}")
			raw("execute if score #fp.made $T = #fp.before $T run function ${refund.asId()}")
		}

		pack.define(pay) {
			raw("scoreboard players set #fp.found $T 0")
			readProperty("eu_per_mob", default = FakePlayer.DEFAULT_EU, min = 1)
			raw("scoreboard players operation #fp.cost $T = #prop $T")
			for (at in SIDES) raw("execute at @s positioned $at as @e[type=minecraft:marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run function ${scanEu.asId()}")
			raw("execute if score #fp.found $T matches 0 run return 0")
			raw("execute store result storage $S pay.amount int 1 run scoreboard players get #fp.cost $T")
			raw("scoreboard players set #fp.took $T 0")
			raw("execute at @s as @e[type=minecraft:marker,tag=$SRC,distance=..2,limit=1] store result score #fp.took $T run function ${take.asId()} with storage $S pay")
			raw("execute at @s run tag @e[type=minecraft:marker,tag=$SRC,distance=..2] remove $SRC")
			raw("data remove storage $S pay")
			raw("execute unless score #fp.took $T = #fp.cost $T run return 0")
			raw("return 1")
		}

		// As a neighbouring electric node: claim it when its grid holds the whole price.
		pack.define(scanEu) {
			raw("execute if score #fp.found $T matches 1 run return 0")
			raw("function ${RaLib.Transport.Net.read.asId()}")
			raw("scoreboard players set #fp.have $T 0")
			raw("execute store result score #fp.have $T run data get storage ra:transport cur.amounts.eu")
			raw("execute unless score #fp.have $T >= #fp.cost $T run return 0")
			raw("scoreboard players set #fp.found $T 1")
			raw("tag @s add $SRC")
		}

		// As the chosen node. `amount` is an int this pack computed, never a player's text.
		pack.define(take) {
			raw("\$return run function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"eu\"}")
		}

		// The price back into the first grid beside the block (the one it was just taken from has
		// that much room again).
		pack.define(refund) {
			raw("scoreboard players set #fp.found $T 0")
			raw("execute store result storage $S pay.amount int 1 run scoreboard players get #fp.cost $T")
			for (at in SIDES) raw("execute at @s positioned $at as @e[type=minecraft:marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run function ${giveBack.asId()}")
			raw("data remove storage $S pay")
		}
		pack.define(giveBack) {
			raw("execute if score #fp.found $T matches 1 run return 0")
			raw("scoreboard players set #fp.found $T 1")
			raw("function ${offer.asId()} with storage $S pay")
		}
		// As the node. `amount` is an int this pack computed.
		pack.define(offer) {
			raw("\$return run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(amount),medium:\"eu\"}")
		}

		registerFakePlayerSpawner()
		registerFakePlayerNatural()
	}

	/**
	 * The sweep and the release, on their own: they also run while the Fake Player is disabled
	 * ([FakePlayer.ENABLED]), so mobs one spawned in a world before that are let go.
	 */
	fun Ra.registerFakePlayerSweep() {
		// From the clock, while some mob is tagged: every SWEEP_RUNS runs, the unheld ones go back.
		pack.define(sweep) {
			raw("scoreboard players add #fp.sweep $T 1")
			raw("execute if score #fp.sweep $T matches ..${FakePlayer.SWEEP_RUNS - 1} run return 0")
			raw("scoreboard players set #fp.sweep $T 0")
			raw("execute as @e[tag=$MOB,tag=!$HELD] run function ${release.asId()}")
			raw("tag @e[tag=$HELD] remove $HELD")
		}
		pack.define(release) {
			raw("execute unless data entity @s CustomName run data modify entity @s PersistenceRequired set value 0b")
			raw("tag @s remove $MOB")
		}
	}
}
