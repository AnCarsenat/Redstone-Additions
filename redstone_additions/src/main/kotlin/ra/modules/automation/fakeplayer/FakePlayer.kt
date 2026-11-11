package ra.modules.automation.fakeplayer

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.readProperty
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.automation.AutomationClock
import ra.modules.automation.fakeplayer.FakePlayerRun.registerFakePlayerRun
import ra.modules.automation.fakeplayer.FakePlayerRun.registerFakePlayerSweep
import ra.core.block.WRENCH_READOUT

/**
 * Fake Player: stands in for a player so a mob farm keeps running while nobody is there.
 *
 * WHY IT CANNOT JUST "BE" A PLAYER. Vanilla natural spawning only happens around real players
 * (within 128 blocks, never within 24), a mob spawner only runs with a real player within its
 * `RequiredPlayerRange` (16), and a mob that is not persistent despawns at once when the nearest
 * player is more than 128 blocks away. A data pack cannot add a player, so the Fake Player does
 * the spawning itself, by the vanilla rules, and only where vanilla is not already doing it.
 * The whole of what it does and does not do is in `docs/automation.md`; how one run goes is
 * [FakePlayerRun], the spawner side [FakePlayerSpawner], the natural side [FakePlayerNatural].
 *
 * DATA. `data.properties.radius` (natural spawning: the cube it spawns in, 1 to [MAX_RADIUS],
 * [ADMIN_MAX_RADIUS] for an admin), `data.properties.max_mobs` (natural spawning: no spawn while
 * that many hostile mobs are within the radius, 1 to [MAX_MOBS], vanilla's monster cap),
 * `data.properties.eu_per_mob` (admin only). `data.data.delay`: the spawner countdown.
 * `data.status.state`, `data.status.spawned`: what the Goggles show.
 *
 * DISABLED (6.0.3). The Fake Player is not a good enough stand-in for a player: its natural
 * spawning uses simplified biome lists and spawns single mobs, not vanilla's packs. Until it is,
 * [ENABLED] is false and the module never registers it: no recipe, no item in `give_all` or the
 * bundle, no advancement, no settings rows, nothing on the automation clock, and none of its
 * functions are written into the pack. The code is kept so it can come back by setting [ENABLED].
 * Only what a world that already has some needs stays on ([registerFakePlayerLeftovers]): a
 * broken block drops its item and loses its marker, and the mobs one spawned are let go by the
 * sweep on the automation clock.
 */
object FakePlayer {
	/** Off: see DISABLED above. Everything that registers or runs the Fake Player checks it. */
	const val ENABLED = false

	val block = CustomBlock(Ns.AUTOMATION, "fake_player", "Fake Player", model = "minecraft:player_head")
	val item = block.loreItem {
		lore(
			loreLine("Keeps a mob farm or a spawner beside it running", "gray"),
			loreLine("with no player around, for EU per mob", "gray"),
			loreLine("Wrench (Shift+RMB): radius", "dark_gray"),
		)
	}

	const val DEFAULT_RADIUS = 8
	const val MAX_RADIUS = 16
	const val ADMIN_MAX_RADIUS = 32
	const val DEFAULT_MAX_MOBS = 20
	/** Vanilla's monster cap. */
	const val MAX_MOBS = 70
	const val DEFAULT_EU = 250
	const val MAX_EU = 100_000

	/** On every mob a Fake Player spawned, while one holds it. */
	const val MOB = "ra.auto.fp_mob"

	/** On a [MOB] a Fake Player held since the last sweep. */
	const val HELD = "ra.auto.fp_held"

	/** A Fake Player holds its mobs within this many blocks (a player keeps mobs within 32). */
	const val HOLD = 48

	/** Clock runs (seconds) between two sweeps: a mob unheld for 30 to 60 seconds goes back to the vanilla rules. */
	const val SWEEP_RUNS = 30

	val T = Obj.TEMP

	val registerBlock = block.fn("register_block")
	val run get() = FakePlayerRun.run
	val sweep get() = FakePlayerRun.sweep
	private val cycleRadius = block.fn("cycle_radius")

	fun Ra.registerFakePlayer() {
		block(block)
		properties(block) {
			number("radius", min = 1, max = MAX_RADIUS, adminMax = ADMIN_MAX_RADIUS, default = DEFAULT_RADIUS)
			number("max_mobs", min = 1, max = MAX_MOBS, default = DEFAULT_MAX_MOBS)
			number("eu_per_mob", min = 1, max = MAX_EU, adminOnly = true, default = DEFAULT_EU)
		}
		wrenchCycle(block.id, WrenchCycle("Radius", "radius", cycleRadius))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		// A player stand-in: the armour stand for the body, the eye of ender for the presence the
		// mobs answer to, gold like a beacon's frame, redstone that runs it.
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.ARMOR_STAND)) {
			pattern("GEG", "GAG", "GRG")
			key("G", Items.GOLD_INGOT)
			key("E", Items.ENDER_EYE)
			key("A", Items.ARMOR_STAND)
			key("R", Items.REDSTONE_BLOCK)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", "{radius:$DEFAULT_RADIUS,max_mobs:$DEFAULT_MAX_MOBS,eu_per_mob:$DEFAULT_EU}")
			setOnNew(block, "data.status", """{state:"starting",spawned:0}""")
			raw("tag ${block.newMarkerSel()} add ${AutomationClock.TAG}")
			finishPlacement(block)
		}

		// Shift+RMB with the Wrench: 4, 8, 12, 16 and round; anything else (an admin's radius) goes back to 4.
		pack.define(cycleRadius) {
			readProperty("radius", default = DEFAULT_RADIUS, min = 1)
			raw("scoreboard players set #fp.r $T 4")
			for (r in listOf(4, 8, 12)) raw("execute if score #prop $T matches $r run scoreboard players set #fp.r $T ${r + 4}")
			raw("execute store result entity @s data.properties.radius int 1 run scoreboard players get #fp.r $T")
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name} radius: ",color:"gray"},{nbt:"data.properties.radius",entity:"@s",color:"aqua"},{text:" blocks",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.4)
			stackedStatusLine("state", "State: ", "aqua")
			stackedPropLine("radius", "Radius: ", "aqua", " blocks")
			stackedPropLine("max_mobs", "Mob cap: ", "aqua")
			stackedPropLine("eu_per_mob", "Per mob: ", "aqua", " EU")
			stackedStatusLine("spawned", "Spawned: ", "green")
		}

		registerBreak()
		registerFakePlayerRun()
	}

	/** While disabled: the break check and the sweep, nothing else (see DISABLED above). */
	fun Ra.registerFakePlayerLeftovers() {
		registerBreak()
		registerFakePlayerSweep()
	}

	private fun Ra.registerBreak() {
		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.stone.break", cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.PLAYER_HEAD)) {}
	}
}
