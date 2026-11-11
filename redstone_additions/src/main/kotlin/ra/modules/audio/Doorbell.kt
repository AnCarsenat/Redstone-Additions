package ra.modules.audio

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.core.ref
import io.github.ayfri.kore.features.tags.blockTag
import ra.modules.audio.AudioPlayer.patternId
import ra.modules.audio.AudioPlayer.statusName
import ra.modules.audio.AudioPlayer.storeArgs
import ra.core.block.WRENCH_READOUT

/**
 * Doorbell: a polished blackstone button that plays its `chime` once per press, heard by every
 * player within `range`. It is a real button, so it also gives the usual redstone pulse.
 *
 * PRESS. `powered=true` on the button is the press: one block test per doorbell per tick. The
 * marker is tagged [RANG] for as long as the button stays down, so a press rings once.
 *
 * PLACEMENT. Like a vanilla button, on the face of the block it was placed against: the block the
 * player looked at (from the placement facing), or else the first neighbour with a full face,
 * floor first. With none, the placement is refused and the item given back.
 * A button whose support goes pops off as in vanilla, which breaks the Doorbell: the custom item
 * drops instead of the plain button.
 */
object Doorbell {
	const val PHYSICAL = "minecraft:polished_blackstone_button"
	val block = CustomBlock(Ns.AUDIO, "doorbell", "Doorbell", model = PHYSICAL, dirType = 3)
	val item = block.loreItem {
		lore(
			loreLine("A button that chimes when pressed", "gray"),
			loreLine("Wrench, Shift+RMB: next chime", "dark_gray"),
		)
	}

	val registerBlock = block.fn("register_block")
	private val look = block.fn("look")
	private val pressed = block.fn("pressed")
	private val ring = block.fn("ring")
	private val cycleChime = block.fn("cycle_chime")

	const val DEFAULT_RANGE = 24
	const val MAX_RANGE = 64
	const val ADMIN_MAX_RANGE = 256

	/** On the marker while its button is down and it has rung. */
	private const val RANG = "ra.au.rang"
	private const val T = Obj.TEMP
	private val chimes = AudioSounds.doorbell

	/** ra.facing order: 0 down .. 5 east, with the offset of the support (opposite the facing). */
	private val faces = listOf(
		Triple(0, "~ ~1 ~", "ceiling"), Triple(1, "~ ~-1 ~", "floor"),
		Triple(2, "~ ~ ~1", "north"), Triple(3, "~ ~ ~-1", "south"),
		Triple(4, "~1 ~ ~", "west"), Triple(5, "~-1 ~ ~", "east"),
	)
	private fun orientFn(f: Int) = ref(Ns.AUDIO, "blocks/doorbell/orient/$f")

	fun Ra.registerDoorbell() {
		block(block)
		properties(block) {
			number("chime", min = 0, max = chimes.size - 1, default = 0, goggles = false)
			number("range", min = 1, max = MAX_RANGE, adminMax = ADMIN_MAX_RANGE, default = DEFAULT_RANGE)
			number("volume", min = 10, max = 100, default = 100)
		}
		wrenchCycle(block.id, WrenchCycle("Chime", "chime", cycleChime))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.NOTE_BLOCK)) {
			pattern("B", "G", "N")
			key("B", Items.POLISHED_BLACKSTONE_BUTTON)
			key("G", Items.GOLD_INGOT)
			key("N", Items.NOTE_BLOCK)
		}

		// As the bat, at the block.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// No full face to hang on: refused, and the item comes back (ra:placement/refuse).
			findSupport()
			raw("execute if score #db.f $T matches -1 run return run function ${ra.hub.placement.refuse.asId()} {why:\"A Doorbell needs the full face of a block to sit on.\"}")
			placeBlock(block)
			for ((f, _, _) in faces) raw("execute if score #db.f $T matches $f run function ${orientFn(f).asId()}")
			setOnNew(block, "data.properties", "{chime:0,range:$DEFAULT_RANGE,volume:100}")
			setOnNew(block, "data.status", """{chime:"${chimes[0].name}"}""")
			finishPlacement(block)
		}
		for ((f, _, face) in faces) pack.define(orientFn(f)) {
			val placer = "@a[tag=${Tag.PLACER},limit=1]"
			if (f >= 2) raw("setblock ~ ~ ~ $PHYSICAL[face=wall,facing=$face] replace")
			else {
				val attach = if (f == 1) "floor" else "ceiling"
				// Floor or ceiling: turned the way the player looks, as vanilla does (yaw 0 = south).
				raw("setblock ~ ~ ~ $PHYSICAL[face=$attach,facing=north] replace")
				for ((yaw, dir) in listOf("-45..45" to "south", "45..135" to "west", "-135..-45" to "east"))
					raw("execute if score $placer ${Obj.YAW} matches $yaw run setblock ~ ~ ~ $PHYSICAL[face=$attach,facing=$dir] replace")
			}
		}
		pack.blockTag("not_sturdy", Ns.AUDIO.id) {
			// The blocks a button falls off: no full face. Not exact (a slab's bottom, a stair's back
			// are full), but a button is never placed where it would pop off the next tick.
			add("#ra_lib:ray_passable")
			for (t in listOf("fences", "fence_gates", "walls", "bars", "slabs", "stairs", "trapdoors", "doors", "beds",
				"campfires", "candle_cakes", "lanterns", "chains", "anvil", "copper_chests", "cauldrons"))
				add("#minecraft:$t")
			add("minecraft:glass_pane")
			for (c in listOf("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
				"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) add("minecraft:${c}_stained_glass_pane")
			for (b in listOf("chest", "trapped_chest", "ender_chest", "cake", "hopper", "bell", "brewing_stand",
				"enchanting_table", "lectern", "grindstone", "stonecutter", "daylight_detector", "scaffolding", "cactus",
				"bamboo", "farmland", "dirt_path", "conduit", "dragon_egg", "pointed_dripstone", "decorated_pot",
				"end_portal_frame", "azalea", "flowering_azalea", "big_dripleaf", "sniffer_egg", "turtle_egg"))
				add("minecraft:$b")
		}

		pack.define(ring) {
			storeArgs(DEFAULT_RANGE, ADMIN_MAX_RANGE)
			patternId("chime", AudioPlayer.DOORBELL_BASE, chimes.size)
			raw("function ${AudioPlayer.start.asId()}")
		}
		pack.define(pressed) {
			raw("execute if entity @s[tag=$RANG] run return 0")
			raw("tag @s add $RANG")
			raw("function ${ring.asId()}")
		}

		// Wrench, Shift+RMB: the next chime, played once. As the marker, at it.
		pack.define(cycleChime) {
			raw("execute store result score #au.c $T run data get entity @s data.properties.chime")
			raw("scoreboard players add #au.c $T 1")
			raw("execute unless score #au.c $T matches 0..${chimes.size - 1} run scoreboard players set #au.c $T 0")
			raw("execute store result entity @s data.properties.chime int 1 run scoreboard players get #au.c $T")
			statusName("chime", "chime", chimes)
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{nbt:"data.status.chime",entity:"@s",color:"aqua"}]""")
			raw("function ${ring.asId()}")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 0.6)
			statusName("chime", "chime", chimes)
			stackedStatusLine("chime", "Chime: ", "aqua")
			stackedPropLine("range", "Range: ", "gray")
			stackedPropLine("volume", "Volume: ", "gray", suffix = "%")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = ra.core.block.STONE_BREAK_SOUND, cloud = true)

		// Every tick, as each marker: one block test while the button is up and nothing plays.
		pack.define(look) {
			raw("execute unless block ~ ~ ~ $PHYSICAL run return run function ${onBreak.asId()}")
			raw("execute if entity @s[tag=${AudioPlayer.PLAYING}] run function ${AudioPlayer.step.asId()}")
			raw("execute if block ~ ~ ~ $PHYSICAL[powered=true] run return run function ${pressed.asId()}")
			raw("tag @s[tag=$RANG] remove $RANG")
		}
		pack.define(block.tick) {
			raw("execute as ${block.markerSel()} at @s run function ${look.asId()}")
		}
	}

	/**
	 * `#db.f ra.temp`: the facing the button gets (ra.facing order), -1 when there is no support.
	 * The support the player aimed at first (opposite the placement facing), then any other
	 * neighbour with a full face: below, above, then the four sides. A face counts as full when the
	 * block is neither air nor in `#ra_audio:not_sturdy`.
	 */
	private fun Function.findSupport() {
		val placer = "@a[tag=${Tag.PLACER},limit=1]"
		fun sturdy(off: String) = "unless block $off #minecraft:air unless block $off #ra_audio:not_sturdy"
		raw("scoreboard players set #db.f $T -1")
		for ((f, off, _) in faces)
			raw("execute if score $placer ${Obj.FACING} matches $f ${sturdy(off)} run scoreboard players set #db.f $T $f")
		for (f in listOf(1, 0, 2, 3, 4, 5))
			raw("execute if score #db.f $T matches -1 ${sturdy(faces[f].second)} run scoreboard players set #db.f $T $f")
	}
}
