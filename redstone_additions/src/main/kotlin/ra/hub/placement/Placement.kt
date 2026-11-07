package ra.hub.placement

import io.github.ayfri.kore.arguments.maths.Axes
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.nearestPlayer
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.tagged.FunctionTagArgument
import io.github.ayfri.kore.commands.execute.Execute
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.features.tags.functionTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Tags
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.ref

/**
 * The placement router. A custom block's item is a bat spawn egg; the bat it spawns is tagged
 * `ra.spawned` + `ra.place.<id>`, and this finds it, checks the spot, and hands it to every
 * registered `handle_placement` through `#ra:placement_handlers`.
 */
internal val detectBats = ref(Ns.RA, "placement/detect_bats")
private val processBat = ref(Ns.RA, "placement/process_bat")
private val handlers = FunctionTagArgument("placement_handlers", Ns.RA.id)

internal fun Ra.registerPlacement() {
	// Built from the registry, in registration order (module order, then block order).
	val registered = placementHandlers
	pack.functionTag("placement_handlers", Ns.RA.id) { registered.forEach { add(it) } }

	// Kept from beet: nothing in the pack reads it, but other packs may.
	pack.blockTag("custom_blocks", Ns.RA.id) {
		add(Blocks.DISPENSER)
		add(Blocks.DROPPER)
	}

	pack.define(detectBats) {
		tag(allEntities { type = EntityTypes.BAT; tag = Tag.SPAWNED; tag = !Tag.PROCESSED }) { add(Tag.PROCESSING) }
		execute {
			asTarget(allEntities { type = EntityTypes.BAT; tag = Tag.PROCESSING })
			at(self())
			run { function(processBat) }
		}
		// Clear the marker so a bat that survived processing is not re-scanned forever.
		tag(allEntities { type = EntityTypes.BAT; tag = Tag.PROCESSING }) { remove(Tag.PROCESSING) }
	}

	pack.define(processBat) { processBat() }
}

private val markerHere = allEntities {
	type = EntityTypes.MARKER
	tag = Tag.CUSTOM_BLOCK
	distance = rangeOrDoubleEnd(0.9)
	limit = 1
}

/** `align xyz positioned ~0.5 ~0.5 ~0.5`: the centre of the block the bat is in. */
private fun Execute.blockCentre() {
	align(Axes.XYZ)
	positioned(vec3(0.5, 0.5, 0.5).relative)
}

/** As the bat, at the bat. */
private fun Function.processBat() {
	tag(self()) { add(Tag.PROCESSED) }

	// The nearest player is the one who spawned the bat.
	tag(nearestPlayer()) { add(Tag.PLACER) }

	// A custom block marker already here: a duplicate placement attempt.
	execute {
		blockCentre()
		ifCondition { entity(markerHere) }
		run { kill(self()) }
	}
	execute {
		blockCentre()
		ifCondition { entity(markerHere) }
		run { returnValue(0) }
	}

	// REPLACEABLE, NOT AIR
	// This asked for #air, which is air, cave air and void air and nothing else. That is not
	// where a spawn egg puts its mob. Vanilla spawns at the clicked block's own position when that
	// block has an EMPTY COLLISION SHAPE, and only at the adjacent position otherwise -- so
	// clicking tall grass, a flower, a rail, a pressure plate or a carpet put the bat inside that
	// block rather than beside it, the test found something that was not air, and the placement
	// was refused. Every custom block in the pack was unplaceable onto anything soft, which is
	// most of a grassy field.
	//
	// #minecraft:replaceable is the tag that means exactly "a placed block may take this space" --
	// the same set vanilla itself overwrites when you place a stone on grass -- so it is the right
	// question, and set_block's `replace` mode then does what the name says.
	execute {
		unlessCondition { block(vec3(), Tags.Block.REPLACEABLE) }
		run { kill(self()) }
	}
	execute {
		unlessCondition { block(vec3(), Tags.Block.REPLACEABLE) }
		run { returnValue(0) }
	}

	// Is this block type switched off in the settings? Checked before the handlers run, so a
	// disabled block never gets as far as creating a marker.
	function(Settings.Placement.check)
	ifBlocked { function(Settings.Placement.refuse) }
	// The placer tag is cleaned up here as well as at the end, because this path returns before
	// ever reaching the end and a stale ra.placer would attach the next placement to the wrong
	// person.
	ifBlocked { tag(allPlayers { tag = Tag.PLACER }) { remove(Tag.PLACER) } }
	ifBlocked { kill(self()) }
	ifBlocked { returnValue(0) }

	// Route to every registered handler; the one whose place tag the bat carries places its block.
	execute {
		blockCentre()
		run { function(handlers) }
	}

	// Configured property defaults, applied to the marker the handler just made.
	execute {
		blockCentre()
		asTarget(markerHere)
		run { function(Settings.Placement.seed) }
	}

	tag(allPlayers { tag = Tag.PLACER }) { remove(Tag.PLACER) }
	kill(self())
}

/** `execute if score #blocked ra.set.tmp matches 1 run <command>`; ra_settings:placement/check writes `#blocked`. */
private fun Function.ifBlocked(command: Function.() -> Unit) {
	execute {
		ifCondition { score(literal("#blocked"), Obj.SET_TMP) equalTo 1 }
		run(command)
	}
}
