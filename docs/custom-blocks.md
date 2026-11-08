# Custom Blocks

A placeable block is a **bat spawn egg**. Placing it spawns a bat tagged `ra`, `ra.spawned` and
`ra.place.<id>`. The placement router (`ra:placement/detect_bats`, in `hub/placement/`) finds the
bat, checks the spot, and runs `#ra:placement_handlers` as the bat at the block centre. The
block's own `handle_placement` returns 1 for its own bat: it places the vanilla block and summons
a marker tagged `ra.custom_block.<id>`, which carries the block's state from then on. The full
runtime flow is in [How It Works](how-it-works.md#3-placement-lifecycle-item-block-marker);
this page is the Kotlin side.

## `CustomBlock`

`ra/core/block/CustomBlock.kt`:

```kotlin
data class CustomBlock(
	val ns: Ns,               // namespace of the block's functions
	val id: String,           // the block id (see below)
	val name: String,         // item name, goggles title
	val model: String,        // item_model of the egg
	val physical: String = model,   // the vanilla block placed in the world
	val dir: String = id,           // folder under blocks/ holding its functions
	val dirType: Int = 0,           // 0 no facing, 1 horizontal, 2 full (ra_lib:orientation)
)
```

`id` is the one string that must never change once worlds exist. It is:

| Used as | Where |
| --- | --- |
| `custom_data={ra:{<id>:1b}}` | on the item: give, recipe result, drop, bundle |
| `ra.place.<id>` | on the bat the egg spawns |
| `ra.custom_block.<id>` | on the marker |
| `data.type` | on the marker, written by `ra_lib:placement/place`; the wrench and read-only registries are keyed by it |

`dir` differs from `id` only where the beet pack put a block's functions in a folder not named
after its id: `ender_item_vault` lives in `ra_ender:blocks/item_vault/`.

### What is derived from it

| Need | Use |
| --- | --- |
| Marker tag / bat tag | `block.tag`, `block.placeTag` |
| `ns:blocks/<dir>/<sub>` | `block.fn("process")`; shortcuts `block.give`, `block.handlePlacement`, `block.tick`, `block.goggles` |
| The item, as a command argument (`give`, item tests) | `block.item` |
| The item, as SNBT (`summon item`, bundle contents) | `block.itemStackNbt(count = 1)` |
| The bat NBT | `block.entityDataNbt()` |
| Item components, inside any `ComponentsScope` | `with(block) { blockItemComponents() }` |
| Selectors | `markers(block)`, `brokenMarkers(block)`, `newMarker(block)` (`core/block/Selectors.kt`) |

The item is `minecraft:bat_spawn_egg` with `item_model`, `item_name`, `custom_data` and
`entity_data` (`{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.<id>"],Silent:1b,NoAI:1b,Invulnerable:1b}`).
`item_name`, `custom_data` and `entity_data` are set as Kore `CustomComponent`s holding raw NBT,
because Kore 2.28's typed versions cannot be written into a recipe's JSON
([Kore Gotchas](gotchas.md#item-components-in-json)).

## The helpers

All in `ra/core/block/`.

| Helper | Generates | Notes |
| --- | --- | --- |
| `pack.blockGive(block)` | `ns:blocks/<dir>/give`: `give @s <block.item>` | |
| `pack.blockRecipe(block, unlockedBy, category = REDSTONE) { shape }` | `ns:<id>` (shaped, `show_notification: true`, result = the block's item) and `ns:recipes/unlock_<id>` | Returns the `RecipeArgument` |
| `pack.shapedRecipe(ns, name, result, unlock, category, count, notify) { shape }` | `ns:<name>` and, when `unlock` is given, `ns:recipes/unlock_<name>` | `result`: `block.recipeResult()`, `spec.recipeResult(item)` or `RecipeResult(item) { components }`; `unlock`: `Unlock(item, criterion = "has_<item>") { match }`; `count`/`category` null and `notify = false` leave the key out |
| `pack.recipeUnlock(name, ns, recipe, unlock)` | `ns:recipes/unlock_<name>` only | For recipes that are not shaped (an `ItemArgument` overload keeps `has_<item>`) |
| `placementGuard(block, checkExisting = true)` | `return 0` unless `@s` carries the place tag, or (when `checkExisting`) if a marker of this type is already within 0.5 | First lines of a handler |
| `placeBlock(block)` | `function ra_lib:placement/place {block_id:<physical>,block_tag:<id>,dir_type:<dirType>}` | Places the block and summons the marker tagged `ra.new` |
| `setOnNew(block, path, snbt)` | `data modify entity <fresh marker> <path> set value <snbt>` | |
| `finishPlacement(block, removePlaceTag = true, withinBlock = true)` | `clearNewTag` (the fresh marker, or every marker of the type), removes the place tag from the bat, `return 1` | Last lines of a handler |
| `pack.blockOnBreak(block, drop, sound, soundListenersOnly, cloud, afterVanillaKill) { cleanup }` | `ns:blocks/<dir>/on_break`: `cleanup`, `killVanillaDrop(physical)`, `afterVanillaKill`, `drop` (default `dropBlockItem(block)`), `breakSound`, `breakCloud`, `kill @s` | Run as the marker at the block |
| `pack.blockTick(block, onBreak, test = BreakTest.Air) { body }` | `ns:blocks/<dir>/tick`: `breakCheck(block, test, onBreak)`, then `body` | `BreakTest.NotBlock(physical)`: broken when the block is anything but `physical` |
| `breakCheck(block, test, onBreak, skipNew)`, `forEachMarker(block, fn)` | `execute as <markers> at @s if/unless block ~ ~ ~ … run function <onBreak>`; `execute as <markers> at @s run function <fn>` | `skipNew` adds `tag=!ra.new` |
| `dropBlockItem(block, tagged = true)`, `dropItem(spec, tagged = false)` | `summon item ~ ~ ~ {Item:<item>[,Tags:["ra","ra.drop_temp"]]}` | |
| `killVanillaDrop(itemId)` | kills the nearest fresh vanilla drop of that id within 2 blocks: count 1, no components, at most 5 ticks old; nothing if none qualifies | Leaves a stack a player dropped nearby, or spilled container contents, alone |

`unlockedBy` names the advancement criterion too: `has_<item path>` (`has_daylight_detector`),
which is what the beet files used.

## Worked example: the Clock

`ra/modules/gates/Clock.kt` is the reference block. Registration:

```kotlin
object Clock {
	val block = CustomBlock(Ns.GATES, "clock", "Clock", model = "minecraft:daylight_detector")
	private const val ON = "ra.clock_on"                    // module-private tag
	val process = block.fn("process")

	fun Ra.registerClock() {
		block(block)                                       // placement handler + goggles route
		properties(block) {                                // its settings: types, bounds, flags
			number("cooldown", min = 2, max = 1200)
			number("delay", readonly = true, visible = false)   // pre-5.1 name, folded into cooldown
		}
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.DAYLIGHT_DETECTOR) {
			pattern("RCR", "SSS")
			key("R", Items.REPEATER); key("C", Items.CLOCK); key("S", Items.STONE)
		}
		pack.define(block.fn("register_block")) { /* debug tellraw */ }
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.define(block.goggles) { goggles() }

		val onBreak = pack.blockOnBreak(block) {
			// the floating clock item, and any pulse still lit
			kill(allEntities { type = EntityTypes.ITEM_DISPLAY; tag = block.tag; distance = rangeOrDoubleEnd(2.0) })
			fill(vec3(-1, -1, -1).relative, vec3(1, 1, 1).relative, Blocks.IRON_BLOCK, Blocks.REDSTONE_BLOCK)
			tag(self()) { remove(ON) }
		}
		pack.blockTick(block, onBreak) {
			execute { asTarget(markers(block)); at(self()); run { function(process) } }
		}
	}
}
```

The handler, as the bat at the block centre:

```kotlin
private fun Function.handlePlacement() {
	placementGuard(block)
	placeBlock(block)
	// block-specific setup, reaching the marker just made through newMarker(block)
	data(newMarker(block)) { modify("data.properties.cooldown") { set(20) } }
	scoreboard.players.set(newMarker(block), Obj.COOLDOWN, 0)
	finishPlacement(block)
}
```

And the module wires it in (`GatesModule.kt`): `registerClock()` in `register()`, `Clock.block`
in the list `ra_gates:load` / `tick` / `items/give_all` iterate, and its item in the `gates`
`GiveBundle`.

### Generated

| Path | From |
| --- | --- |
| `ra_gates:blocks/clock/give` | `blockGive` |
| `ra_gates:clock` (recipe), `ra_gates:recipes/unlock_clock` | `blockRecipe` |
| `ra_gates:blocks/clock/handle_placement`, `process`, `goggles`, `register_block` | `define` |
| `ra_gates:blocks/clock/on_break`, `tick` | `blockOnBreak`, `blockTick` |
| entry in `#ra:placement_handlers` | `ra.block` → Hub |
| line in `ra:tools/goggles/draw_block` | `ra.block` → Tools |

## Breaking: the standard shape and its variants

The beet pack spelled break handling as one line per cleanup step:

```mcfunction
execute as @e[type=marker,tag=ra.custom_block.clock] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.clock] at @s run kill @e[type=item,...]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.clock] at @s run kill @e[type=item_display,...]
...
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.clock] remove ra.broken
```

`blockTick` + `blockOnBreak` route each broken marker through one `on_break` function instead:
the same work, the cleanup in one place, no `ra.broken` tag carried between lines.
`compare_reference.py` reports these ticks as **DIFFERS** and `on_break` as **EXTRA**; that is
[expected](porting-history.md#differences-that-are-expected).

Not every beet block broke the same way, and where the difference is observable the port keeps
it, through the options of the core helpers:

| Variant | Where | Spelled |
| --- | --- | --- |
| timing gates | `timingOnBreak` / `timingTick` in `modules/gates/GateCommon.kt` (Delayer, Extender, Shortener, Randomizer) | `blockOnBreak(block, drop = { dropBlockItem(block, tagged = false) }, sound, cloud = true, afterVanillaKill = { outputShell(false) })`, `blockTick(block, onBreak, BreakTest.NotBlock(physical))` |
| blocks with a full `ItemSpec` item (Infinite generators, Chunk Loader, sensors, wireless, ender) | each block's file | `breakCheck(block, physical, onBreak, skipNew)`, `blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = …)` |
| shared `ra_wires:blocks/handle_placement` | `modules/wires/blocks/Placement.kt` + `PlacementSpecs.kt` | one handler for every Wires block, driven by a per-place-tag spec list (`placementHandlers += Placement.handlePlacement`); blocks register with `ra.block(b, placement = false, goggles = false)`, and goggles routes are added per *marker* type, because several items (the retired pipe tiers) place the same marker |

!!! warning "Why the timing gates drop untagged items"
    Some interactive blocks overwrite the first `ra.drop_temp` item they find nearby with their
    container contents. A gate drop tagged `ra.drop_temp` lying next to one could be replaced.
    The beet pack summoned these drops untagged, and the port keeps that.

## Items richer than `CustomBlock`

`CustomBlock` carries the four components every egg has. Items with more (lore, rarity,
`enchantment_glint_override`, food, consumable) use one of these models:

| Model | Where | Used by |
| --- | --- | --- |
| `ItemSpec` + the `Snbt` value classes, `CustomBlock.loreItem { }` | `core/item/` | Chunk Loader, Infinite generators and casing/cores, Jetpack kits, sensors, wireless, ender, interactive, storage; `ItemSpec.fromSnbt` takes SNBT text copied from a `give` |
| `WiresItem` (`block`, `lore`, `epic`, `recipe`) | `modules/wires/WiresBlocks.kt` | every Wires item |
| `ToolItem` (`id`, `spec`: an `ItemSpec.fromSnbt`) | `tools/ToolItems.kt` | the six tools |
| `BaseTier` | `modules/multiblock/blocks/BaseTier.kt` | the five Multiblock Base tiers (one block type, five items) |

Each writes the same item in every shape the pack needs (give argument, SNBT stack, recipe
result) from one description, which is the property that matters. They exist side by side
because `CustomBlock` does not yet carry extra components; folding them into the core is open
work.

## Goggles readout

Every block's `goggles` function both draws its billboard and publishes its name, because
`ra:tools/block_name` runs the same dispatch with `storage ra:temp name_only` set to answer the
Data Handlers. The helpers in `core/block/GogglesLines.kt` keep that contract:

```kotlin
private fun Function.goggles() {
	gogglesHeader(block)   // 1. name → ra:temp block_name  2. return if name_only  3. name line
	propLine("cooldown", "Cooldown: ", "aqua", y = 0.45, suffix = "t")
}
```

| Helper | Macro called | Reads |
| --- | --- | --- |
| `propLine(path, label, color, y, suffix)` | `prop_line` | `data.properties.<path>` |
| `dataLine(path, label, color, y, suffix)` | `data_line` | `data.status.<path>` |
| `textLine(label, value, color, y, suffix)` | `text_line` | the literal `value` |
| `stackReset(top, step)` | `stack_reset` | nothing any more (kept so old callers load) |
| `stackedPropLine(…)` | `stacked_prop_line` | `data.properties` |
| `stackedStatusLine(…)` | `stacked_status_line` | `data.status` |
| `stackedDataLine(…)` | `stacked_data_line` | **`data.data`** (private working state) |
| `stackedTextLine(…)` | `stacked_text_line` | a literal |

Each helper appends one line to the block's billboard, in call order (name first, then top to
bottom); `goggles/draw_block` summons it afterwards as **one** `text_display`, 0.3 above the
block's top face, growing upward with the line count (0.1 block per line at the billboard's
0.4 scale). The `y` / `nameY` arguments are accepted and ignored. A property line is drawn only
when the property's `goggles` flag allows it ([Property schemas](tools-and-registries.md#property-schemas)).
A missing value renders "N/A" in red; asking the wrong helper (`stackedDataLine` for a status
value) is the usual cause, and it is silent.

## Checklist: adding a block

1. A `CustomBlock` in a new file `ra/modules/<module>/<Block>.kt`.
2. `ra.block(block)` (or `placement = false` behind a shared handler).
3. `blockGive`, `blockRecipe` (or `recipeUnlock` for a custom recipe).
4. `handle_placement` with `placementGuard` / `placeBlock` / `finishPlacement`.
5. `on_break` and `tick` (`blockOnBreak` / `blockTick` unless the block needs a variant).
6. `goggles` with `gogglesHeader` + lines.
7. Add it to the module's `load` / `tick` / `items/give_all` list and its `GiveBundle`.
8. `properties(block) { ... }`: every property, its type and flags (read-only, admin-only,
   hidden) — see [Property schemas](tools-and-registries.md#property-schemas). Wrench rows: see
   [Tools and Registries](tools-and-registries.md).
9. A settings `BlockRow` (and `PropRow` defaults) on the module's page: see [Settings](settings-internals.md).
10. `./gradlew koreBuild`: the validator catches any call left dangling.

Nothing shared is edited: no placement tag, no goggles dispatch, no bundle function.
