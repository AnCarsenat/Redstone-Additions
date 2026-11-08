package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.features.tags.entityTypeTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.placeBlock
import ra.core.block.stackedDataLine
import ra.core.block.stackedPropLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.modules.interactive.BigTorchSweep.registerBigTorchSweep
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage
import ra.core.block.forEachMarker
import ra.core.block.breakSound
import ra.core.block.placementGuard
import ra.core.block.dropItem
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/**
 * Big Torch: an end rod drawn as an oversized torch that denies its area to hostile mobs that
 * spawn inside it (see [BigTorchSweep]).
 */
object BigTorch {
	val block = CustomBlock(Ns.INTERACTIVE, "big_torch", "Big Torch", model = "minecraft:torch", physical = "minecraft:end_rod")
	val item = block.loreItem {
		lore(loreLine("Stops hostile mobs spawning nearby", "gray"), loreLine("Set the radius with the Data Handler", "dark_gray"))
	}
	val registerBlock = block.fn("register_block")
	val skin = block.fn("skin")

	/** Mobs the torch denies. */
	const val SPAWN_BLOCKED = "spawn_blocked"

	fun Ra.registerBigTorch() {
		block(block)
		properties(block) {
			number("radius", min = 1, max = 100)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		// Crafted from the Enchanted Coal Block (a barrier, see EnchantedCoal) and a stick. The beet
		// pack gave it no unlock advancement and no recipe notification; kept that way.
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), unlock = null, notify = false) {
			pattern("B", "S")
			key("B", listOf(Items.BARRIER))
			key("S", listOf(Items.STICK))
		}

		pack.entityTypeTag(SPAWN_BLOCKED, Ns.INTERACTIVE.id) {
			for (e in listOf(
				EntityTypes.ZOMBIE, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.HUSK, EntityTypes.DROWNED,
				EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.WITHER_SKELETON,
				EntityTypes.CREEPER, EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER, EntityTypes.ENDERMAN,
				EntityTypes.ENDERMITE, EntityTypes.SILVERFISH, EntityTypes.WITCH, EntityTypes.SLIME,
				EntityTypes.MAGMA_CUBE, EntityTypes.BLAZE, EntityTypes.GHAST, EntityTypes.ZOMBIFIED_PIGLIN,
				EntityTypes.PIGLIN, EntityTypes.PIGLIN_BRUTE, EntityTypes.HOGLIN, EntityTypes.GUARDIAN,
				EntityTypes.ELDER_GUARDIAN, EntityTypes.PHANTOM, EntityTypes.PILLAGER, EntityTypes.VINDICATOR,
				EntityTypes.EVOKER, EntityTypes.ILLUSIONER, EntityTypes.RAVAGER, EntityTypes.BREEZE,
				EntityTypes.CREAKING,
			)) add(e)
		}

		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(block.goggles) { goggles() }
		pack.define(skin) { skin() }
		val sweep = registerBigTorchSweep()

		// The vanilla drop goes, and the Big Torch item is summoned in its place. This ran bare, so
		// it killed whatever end rod happened to be within two blocks of wherever the tick function
		// stood -- never the one that had just dropped out of the torch. Breaking a Big Torch handed
		// back both the item and an end rod.
		//
		// Hand-composed rather than blockOnBreak: the skin is cleared AFTER the sound, and
		// blockOnBreak runs its cleanup first.
		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropItem(item)
			breakSound(listenersOnly = true)
			function(RaLib.Skin.clear, nbt { this["id"] = block.id })
			kill(self())
		}

		pack.define(block.tick) {
			breakCheck(block, Blocks.END_ROD, onBreak)

			// Redraw a skin that has gone -- a chunk reloaded, a display killed by hand, or a torch
			// built by the build that used a shroomlight. Cheap: skin leaves after one selector when
			// the display is already there.
			forEachMarker(block, skin)

			// THE SWEEP RUNS EVERY 10 TICKS, NOT EVERY TICK. Its selector reaches as far as the
			// radius does, which is up to 100 blocks, and there is no reason for it to be prompt: a
			// mob that lives half a second before being denied is indistinguishable from one that
			// never spawned. Paying a 100-block entity selector per torch per tick to shorten that
			// would be the most expensive thing in the pack.
			scoreboard.players.add(literal("#big_torch.scan"), Obj.TEMP, 1)
			raw("execute unless score #big_torch.scan ${Obj.TEMP} matches 10.. run return 0")
			scoreboard.players.set(literal("#big_torch.scan"), Obj.TEMP, 0)
			forEachMarker(block, sweep)
		}
	}

	/** As the spawned bat, at its position. */
	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		// An end rod, and always standing up.
		//
		// dir_type 0 places the block plain, and an end rod's default facing is `up`, so vertical
		// is what you get without a facing to resolve -- which is the whole requirement. A torch
		// lying on its side would also put its flame inside the block beside it.
		//
		// The end rod is the light: it gives off level 14 on its own, so the block lights its area
		// whether or not the display over it ever renders.
		placeBlock(block)
		raw("data modify entity @e[type=marker,tag=${block.tag},tag=${Tag.NEW},limit=1,sort=nearest] data.properties set value {radius:16}")
		function(skin)
		finishPlacement(block, removePlaceTag = false, withinBlock = false)
	}

	private fun Function.goggles() {
		gogglesHeader(block, nameY = 1.0)
		stackedPropLine("radius", "Radius: ", "aqua", " blocks")
		stackedDataLine("removed", "Denied: ", "green", " mobs")
	}

	/**
	 * Draw the oversized torch over the end rod, or repair it if it went missing. As the torch
	 * marker, at the block position.
	 *
	 * WHY NOT ra_lib:skin/apply. That draws one full block over another at a fixed 1.02, which is
	 * exactly right for a barrel wearing a dispenser and no use here: a torch is not a full block
	 * and the whole point is that it is bigger than the one vanilla draws. The scale below is
	 * worked out from the two models rather than borrowed.
	 *
	 * THE NUMBERS
	 *   torch model    x,z 7..9   y 0..10   -> half-width 0.0625, height 0.625
	 *   end rod model  x,z 6..10  y 0..16   -> half-width 0.125,  height 1.0
	 *
	 * The torch has to ENCLOSE the end rod, not sit inside it. Two surfaces that share a plane
	 * z-fight, and the end rod's shaft is exactly as wide as a torch -- drawn at 1:1 they would be
	 * the same 2x2 pixels and fight the whole way up.
	 *
	 * Height: 1.0 plus the pack's usual 0.01 of clearance per face is 1.02, so the scale is
	 * 1.02 / 0.625 = 1.632. The torch is therefore one block tall to look at and a hundredth over
	 * it in fact, which is the same margin ra_lib:skin/spawn uses and for the same reason -- two
	 * thousandths reads as coplanar to the depth buffer a few chunks out and flickers.
	 *
	 * Width: the end rod is 0.125 out from the middle, so the torch needs 0.135, and
	 * 0.135 / 0.0625 = 2.16. Rounded to 2.2 for a little more room. The end rod ends up entirely
	 * inside the torch, which is why none of it pokes through.
	 *
	 * translation centres the scaled model: -0.5 * scale on x and z, and on y the bottom is dropped
	 * to the block floor less the same clearance.
	 */
	private fun Function.skin() {
		// Anchored to the block centre, which is where skin_spawn stands the display. The marker is
		// nominally there too, but "nominally" is what went wrong with the item pipe's filter
		// display: it was summoned 0.9 above the marker and looked for again within 0.6, so it
		// never found its own and stacked a new one on every pass. Both ends of an ownership test
		// have to agree on the anchor.
		raw("execute align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[type=block_display,tag=ra.skin.big_torch,distance=..0.4] run return 0")

		// Summoned straight from here rather than through a macro function. There is nothing to
		// substitute: a torch is a light source and is drawn at full brightness whatever the light
		// around it, so there is no sampled value to pass. It WAS a macro, with a `$` line carrying
		// no `$(...)` in it at all, and Minecraft rejects that outright -- "No variables in macro"
		// -- so the function failed to load and no torch was ever drawn.
		//
		// Centre-anchored, the same place the ownership test above looks, so the two agree about
		// which display belongs to this block.
		//
		// Lit as a torch rather than sampled from the world: this IS the light, and a display that
		// dimmed at night would be the one thing in the build that looked switched off.
		// `block_state:{id:...}` is the 26.3 spelling (`Name` before).
		raw("""execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["${Tag.RA}","${Tag.DISPLAY}","ra.skin","ra.skin.big_torch"],brightness:{sky:15,block:15},block_state:{id:"minecraft:torch"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-1.1f,-0.51f,-1.1f],scale:[2.2f,1.632f,2.2f]}}""")
	}
}
