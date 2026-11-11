package ra.modules.automation.conveyor

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.readProperty
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedTextLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.lib.orientation.Orientation
import ra.modules.interactive.FanPush

/**
 * Conveyor: a smooth stone slab that carries what stands on it, items, mobs and players, the way
 * its arrow points, at `speed`. How one push goes is [ConveyorPush].
 *
 * THE BLOCK. A bottom smooth stone slab, so what it carries sits half a block up like on a real
 * belt, with a flat block display of magenta glazed terracotta on top as the arrow. The arrow of
 * that texture points away from the block's `facing`, and the marker's `ra.facing` faces the
 * player who placed it, so the display takes the marker's facing as is and the push goes the
 * other way: away from the player, the way they were looking. Only the four horizontal facings:
 * placing while looking up or down still uses the look's compass direction.
 *
 * COST. Every [POLL] ticks one box test per Conveyor over the half block above it, which tags the
 * ones with something on them ([ACTIVE]); an idle Conveyor stops there. Only while some are
 * active, each tick every active Conveyor pushes what is on it (in small steps, so players move
 * smoothly) and tags the Conveyor it feeds, so a chain hands things on without waiting for the
 * next poll. With none active the per-tick work is the break check every block has and one score
 * test.
 *
 * DATA. `data.properties.speed`: 1 to [MAX_SPEED] ([ADMIN_MAX_SPEED] for an admin), 0.05 blocks a
 * tick per step (one block a second).
 */
object Conveyor {
	const val PHYSICAL = "minecraft:smooth_stone_slab"
	private const val ARROW_BLOCK = "minecraft:magenta_glazed_terracotta"

	val block = CustomBlock(Ns.AUTOMATION, "conveyor", "Conveyor", model = ARROW_BLOCK, physical = PHYSICAL, dirType = 3)
	val item = block.loreItem {
		lore(
			loreLine("Carries items, mobs and players along", "gray"),
			loreLine("the way its arrow points", "gray"),
			loreLine("Wrench (Shift+RMB): speed", "dark_gray"),
		)
	}

	const val DEFAULT_SPEED = 2
	const val MAX_SPEED = 5
	const val ADMIN_MAX_SPEED = 10

	/** Ticks between two looks for what stands on the Conveyors. */
	const val POLL = 5

	/** On a Conveyor's marker while something stands on it. */
	const val ACTIVE = "ra.auto.cv_active"

	/** On the arrow display. */
	const val ARROW = "ra.auto.cv_arrow"

	val T = Obj.TEMP
	private const val PHASE = "#cv.phase"
	const val ON = "#cv.on"

	val registerBlock = block.fn("register_block")
	private val cycleSpeed = block.fn("cycle_speed")
	private val poll = block.fn("poll")
	private val activate = block.fn("activate")

	/** `ra.facing` order: the facing the arrow display is drawn with. Only 2..5 are used. */
	private val FACINGS = listOf(2 to "north", 3 to "south", 4 to "west", 5 to "east")

	fun Ra.registerConveyor() {
		block(block)
		properties(block) {
			number("speed", min = 1, max = MAX_SPEED, adminMax = ADMIN_MAX_SPEED, default = DEFAULT_SPEED)
		}
		wrenchCycle(block.id, WrenchCycle("Speed", "speed", cycleSpeed))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		// Shaped like a rail: a belt of dried kelp on an iron frame with the redstone that drives it.
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.DRIED_KELP), count = 6) {
			pattern("KKK", "IRI")
			key("K", Items.DRIED_KELP)
			key("I", Items.IRON_INGOT)
			key("R", Items.REDSTONE)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			// Horizontal only: dir_type 3 gives a vertical facing for a look up or down, so the
			// facing is read again the horizontal way (the default, south, without a placer).
			raw("scoreboard players set #facing $T 3")
			raw("execute as @p[tag=ra.placer] run function ${Orientation.getFacing.asId()} {dir_type:1}")
			raw("execute store result score ${block.newMarkerSel()} ${Obj.FACING} run scoreboard players get #facing $T")
			raw("data modify entity ${block.newMarkerSel()} Rotation set from storage ra:temp Rotation")
			setOnNew(block, "data.properties", "{speed:$DEFAULT_SPEED}")
			for ((f, name) in FACINGS) {
				raw("execute if score #facing $T matches $f align xyz run summon minecraft:block_display ~ ~ ~ " +
					"{Tags:[\"ra\",\"ra.display\",\"$ARROW\"],block_state:{id:\"$ARROW_BLOCK\",properties:{facing:\"$name\"}}," +
					"transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0.501f,0f],scale:[1f,0.001f,1f]}}")
			}
			finishPlacement(block)
		}

		// Shift+RMB with the Wrench: 1 to 5 and round; anything else (an admin's speed) goes back to 1.
		pack.define(cycleSpeed) {
			readProperty("speed", default = DEFAULT_SPEED, min = 1)
			raw("scoreboard players set #cv.s $T 1")
			for (s in 1 until MAX_SPEED) raw("execute if score #prop $T matches $s run scoreboard players set #cv.s $T ${s + 1}")
			raw("execute store result entity @s data.properties.speed int 1 run scoreboard players get #cv.s $T")
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name} speed: ",color:"gray"},{nbt:"data.properties.speed",entity:"@s",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			stackedPropLine("speed", "Speed: ", "aqua")
			stackedTextLine("", "Carries things the way the arrow points", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true, cleanup = {
			// The arrow stands on the block's corner (summoned `align xyz`), 0.87 from its centre.
			raw("execute align xyz run kill @e[type=minecraft:block_display,tag=$ARROW,distance=..0.01]")
		})

		pack.define(poll) {
			raw("scoreboard players set $ON $T 0")
			raw("tag @e[type=minecraft:marker,tag=$ACTIVE] remove $ACTIVE")
			executeRun("as ${block.markerSel()} at @s align xyz positioned ~ ~0.5 ~ if entity @e[${ConveyorPush.MOVABLE},dx=0,dy=0,dz=0,limit=1]", activate)
		}
		pack.define(activate) {
			raw("tag @s add $ACTIVE")
			raw("scoreboard players add $ON $T 1")
		}

		pack.define(block.tick) {
			// A double slab (another slab put on top) or anything else than the bottom slab is a break.
			raw("execute as ${block.markerSel()} at @s unless block ~ ~ ~ $PHYSICAL[type=bottom] run function ${onBreak.asId()}")
			raw("scoreboard players add $PHASE $T 1")
			raw("execute if score $PHASE $T matches $POLL.. run scoreboard players set $PHASE $T 0")
			executeRun("if score $PHASE $T matches 0", poll)
			raw("execute if score $ON $T matches 1.. store result score ${FanPush.NOW} $T run time query gametime")
			executeRun("if score $ON $T matches 1.. as @e[type=minecraft:marker,tag=$ACTIVE] at @s", ConveyorPush.run)
		}

		ConveyorPush.register(this)
	}

	/** The speed, capped at the admin maximum, into `#prop ra.temp`. As the marker. */
	internal fun io.github.ayfri.kore.functions.Function.readSpeed() =
		readCappedProperty("speed", default = DEFAULT_SPEED, min = 1, adminMax = ADMIN_MAX_SPEED)
}
