package ra.modules.automation.absorber

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Admin.readCappedProperty
import ra.core.Ns
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
import ra.modules.automation.absorber.XpAbsorberRun.registerXpAbsorberRun
import ra.core.block.WRENCH_READOUT

/**
 * Experience Absorber: collects the experience orbs around it and sends them, as liquid
 * experience, into a fluid network touching it. How one look goes is [XpAbsorberRun].
 *
 * DATA. `data.properties.radius`: how far it reaches, 1 to [MAX_RADIUS] blocks ([ADMIN_MAX_RADIUS]
 * for an admin). `data.status.state`, `data.status.absorbed` (points so far): what the Goggles show.
 */
object XpAbsorber {
	val block = CustomBlock(Ns.AUTOMATION, "experience_absorber", "Experience Absorber", model = "minecraft:verdant_froglight")
	val item = block.loreItem {
		lore(
			loreLine("Collects experience orbs around it", "gray"),
			loreLine("into a pipe or tank beside it, as liquid experience", "gray"),
			loreLine("Wrench (Shift+RMB): radius", "dark_gray"),
		)
	}

	const val DEFAULT_RADIUS = 4
	const val MAX_RADIUS = 8
	/** A selector's sphere grows with the cube of its radius. */
	const val ADMIN_MAX_RADIUS = 16

	val registerBlock = block.fn("register_block")
	val run get() = XpAbsorberRun.run
	private val cycleRadius = block.fn("cycle_radius")

	fun Ra.registerXpAbsorber() {
		block(block)
		properties(block) {
			number("radius", min = 1, max = MAX_RADIUS, adminMax = ADMIN_MAX_RADIUS, default = DEFAULT_RADIUS)
		}
		wrenchCycle(block.id, WrenchCycle("Radius", "radius", cycleRadius))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		// Shaped like a cauldron-side machine: a hopper that gathers, the lapis of enchanting above
		// it, a bucket for the liquid below, a copper frame like the pipes.
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.HOPPER)) {
			pattern("CLC", "CHC", "CBC")
			key("C", Items.COPPER_INGOT)
			key("L", Items.LAPIS_BLOCK)
			key("H", Items.HOPPER)
			key("B", Items.BUCKET)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", "{radius:$DEFAULT_RADIUS}")
			setOnNew(block, "data.status", """{state:"no experience around",absorbed:0}""")
			raw("tag ${block.newMarkerSel()} add ${AutomationClock.TAG}")
			finishPlacement(block)
		}

		// Shift+RMB with the Wrench: 2, 4, 6, 8 and round; anything else (an admin's radius) goes
		// back to 2. As the marker, at it.
		pack.define(cycleRadius) {
			readProperty("radius", default = DEFAULT_RADIUS, min = 1)
			raw("scoreboard players set #xa.r ra.temp 2")
			for (r in listOf(2, 4, 6)) raw("execute if score #prop ra.temp matches $r run scoreboard players set #xa.r ra.temp ${r + 2}")
			raw("execute store result entity @s data.properties.radius int 1 run scoreboard players get #xa.r ra.temp")
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name} radius: ",color:"gray"},{nbt:"data.properties.radius",entity:"@s",color:"aqua"},{text:" blocks",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedPropLine("radius", "Radius: ", "aqua", " blocks")
			stackedStatusLine("absorbed", "Absorbed: ", "green", " points")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.froglight.break", cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.VERDANT_FROGLIGHT)) {}

		registerXpAbsorberRun()
	}

	/** Use time: the radius, capped at the admin maximum, into `#prop ra.temp`. As the marker. */
	internal fun io.github.ayfri.kore.functions.Function.readRadius() =
		readCappedProperty("radius", default = DEFAULT_RADIUS, min = 1, adminMax = ADMIN_MAX_RADIUS)
}
