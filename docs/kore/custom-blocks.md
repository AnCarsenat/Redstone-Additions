# Custom Blocks

A placeable block is a bat spawn egg. Placing it spawns a bat tagged `ra.spawned` and
`ra.place.<id>`; the placement router (`ra:placement/detect_bats`) runs every function in
`#ra:placement_handlers` as that bat, and the block's own handler places the vanilla block and
summons a marker tagged `ra.custom_block.<id>`. The marker carries the block's state from then
on. [How It Works](../how-it-works.md#3-placement-lifecycle-item-block-marker) has the full flow.

## Describing a block

```kotlin
val block = CustomBlock(
	ns = Ns.GATES,
	id = "clock",                              // custom_data key, ra.place.<id>, ra.custom_block.<id>, data.type
	name = "Clock",                            // item name, goggles title
	model = "minecraft:daylight_detector",     // item_model of the egg
	physical = "minecraft:daylight_detector",  // block placed in the world (defaults to model)
	dirType = 0,                               // 0 none, 1 horizontal, 2 full facing
)
```

`dir` overrides the folder under `blocks/` when it differs from the id
(`ender_item_vault` lives in `ra_ender:blocks/item_vault/`).

Everything that must agree on the block's identity is derived from this value:

| Need | Derived as |
|---|---|
| give command / item tests | `block.item` |
| item embedded in NBT (drops, bundles) | `block.itemStackNbt()` |
| recipe result | `blockRecipe(...)` |
| marker tag / bat tag | `block.tag` / `block.placeTag` |
| function paths | `block.fn("process")`, `block.give`, `block.tick`, `block.goggles`, `block.handlePlacement` |
| selectors | `markers(block)`, `brokenMarkers(block)`, `newMarker(block)` |

## Writing one: the Clock

`ra/modules/gates/Clock.kt` is the reference. Its shape:

```kotlin
object Clock {
	val block = CustomBlock(Ns.GATES, "clock", "Clock", model = "minecraft:daylight_detector")
	val process = block.fn("process")

	fun Ra.registerClock() {
		block(block)                                  // registry: placement handler + goggles
		pack.blockGive(block)                         // blocks/clock/give
		pack.blockRecipe(block, unlockedBy = Items.DAYLIGHT_DETECTOR) {
			pattern("RCR", "SSS")
			key("R", Items.REPEATER); key("C", Items.CLOCK); key("S", Items.STONE)
		}                                             // recipe + recipes/unlock_clock
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.define(block.goggles) { goggles() }

		val onBreak = pack.blockOnBreak(block) {      // blocks/clock/on_break
			/* block-specific cleanup */
		}
		pack.blockTick(block, onBreak) {              // blocks/clock/tick
			execute { asTarget(markers(block)); at(self()); run { function(process) } }
		}
	}

	private fun Function.handlePlacement() {
		placementGuard(block)        // not our bat / already a block here -> return 0
		placeBlock(block)            // ra_lib:placement/place {block_id, block_tag, dir_type}
		/* setup on newMarker(block) */
		finishPlacement(block)       // drop ra.new + the place tag, return 1
	}
}
```

The module object (`GatesModule`) calls `registerClock()` and lists the block's `tick` in the
module `tick` function.

### Breaking

`blockTick(block, onBreak) { body }` writes the type's tick: every marker whose block became
air runs `on_break`, then `body` runs. `blockOnBreak(block) { cleanup }` writes `on_break`:
your cleanup, then the vanilla drop is removed, the custom item dropped and the marker killed.

The beet version wrote break handling as one `execute as @e[tag=ra.broken,…]` line per cleanup
step, with a `ra.broken` tag to carry state between them. One function per broken marker does
the same work with the cleanup in one place; `compare_reference.py` reports these ticks as
different and `on_break` as extra, which is expected.

### Goggles

A block's `goggles` function both draws its readout and publishes its name (the Data Handlers
ask for names through the same function). Use the helpers in `core/block/GogglesLines.kt`:

```kotlin
private fun Function.goggles() {
	gogglesHeader(block, nameY = 0.7)      // name, name_only early return, name billboard
	propLine("cooldown", "Cooldown: ", "aqua", y = 0.45, suffix = "t")
}
```

`propLine` reads `data.properties`, `dataLine` reads `data.status`. For more than three lines,
`stackReset(top, step)` then the `stacked*Line` helpers lay the ladder out for you — and note
`stackedDataLine` reads `data.data`, not `data.status` (`stackedStatusLine` does that).

### Recipes

`blockRecipe` writes the shaped recipe `ns:<id>` (category `redstone` unless given, with
`show_notification`) and the advancement `ns:recipes/unlock_<id>` that grants it once the player
holds `unlockedBy`. Use `recipeUnlock(...)` alone for recipes that do not produce a block
(tools, kits).

## Registering what tools need

| The block has… | Register |
|---|---|
| properties the wrench cycles | `ra.wrenchCycle(block.id, WrenchCycle("Mode", "mode", cycleFn))` |
| a plain-RMB wrench toggle | `ra.wrenchToggles += TagRoute(block.tag, toggleFn)` |
| properties no tool may edit | `ra.readonly(block.id, "generation_rate")` |
| properties the Data Handlers list | `ra.dataHandlerProperties("cooldown")` |
| settings defaults | a row in its module's settings page (`ra.settings`) |

## Item-components gotcha

Kore 2.28's typed `itemName`, `customData` and `entityData` components serialize only to SNBT;
inside a recipe result's JSON they throw at generation time. `CustomBlock` therefore sets those
three as Kore `CustomComponent`s holding raw NBT, which serialize to both. Do the same for any
item that appears in a data file.
