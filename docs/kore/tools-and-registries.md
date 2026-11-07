# Tools and Registries

The tools (`ra.tools`, namespace `ra`) read and edit blocks they know nothing about at compile
time. They learn about blocks through the [`Ra` registry](architecture.md#the-registry): a block
registers what it offers, and `ToolsPart`, which runs after every module, builds the shared
functions from what was registered. This page is the contract on both sides. For what each tool
does in game, see [Tools](../tools.md).

## Goggles

| Registry | Built into | Shape of each generated line |
| --- | --- | --- |
| `ra.goggles` (`TagRoute(markerTag, fn)`) | `ra:tools/goggles/draw_block` | `execute if entity @s[tag=<markerTag>] run function <fn>` |
| `ra.multiblockGoggles` | `ra:tools/goggles/draw_multiblock` | same, for multiblock markers |

`ra.block(block)` adds `TagRoute(block.tag, block.goggles)` for you. Add a route by hand only
when the readout is not one per item: Wires adds one per **marker** type, because several items
(the retired pipe tiers) place the same marker.

`ra:tools/block_name` runs `draw_block` with `storage ra:temp name_only` set, so the Data
Handlers get a block's display name from the same function that draws it. That is why every
`goggles` function must publish the name first and return early on `name_only`; `gogglesHeader`
does both ([Custom Blocks](custom-blocks.md#goggles-readout)).

Billboard writers callable from a readout:

| Reference | Path | For |
| --- | --- | --- |
| `Goggles.Billboard.*` (`ra/api/ToolsApi.kt`) | `ra:tools/goggles/billboard/{handle_billboard, prop_line, data_line, text_line, item_line, show_io_line, stack_reset, stacked_*_line, init_offsets}` | block readouts; wrapped by `core/block/GogglesLines.kt` |
| `Goggles.Billboard.*` (same object) | `handle_multiblock`, `show_input`, `show_output`, `show_control`, `show_literal_line`, `render_*`, `stack_next` | multiblock readouts and the tools' own rendering |
| `Goggles.Status.readStatusMultiblock` | `ra:tools/goggles/status/read_status_multiblock` | multiblock status |

## Wrench

### Shift+RMB: cycling

A block declares the properties the wrench may cycle, keyed by its marker's `data.type`:

```kotlin
// ra/modules/wires/WiresBlocks.kt
wrenchCycle("electric_consumer", WrenchCycle("Rate", "cooldown", ElectricConsumer.cycleRate))
wrenchCycle("liquid_drain",
	WrenchCycle("Mode", "mode", LiquidDrain.cycleMode),
	WrenchCycle("Rate", "cooldown", LiquidDrain.cycleRate))
```

| Field | Meaning |
| --- | --- |
| `label` | Row name in the menu |
| `prop` | `data.properties.<prop>`, shown as the current value |
| `fn` | The cycler. Runs **as the marker, at the block**; messages `@a[distance=..10]` |

`ra:tools/wrench/init_registry` (`tools/wrench/WrenchRegistry.kt`) writes every row into
`storage ra:wrench cyclables`, keys sorted, on every load:

```mcfunction
data modify storage ra:wrench cyclables set value {"creative_fluid":[{label:"Medium",prop:"medium",fn:"ra_wires:blocks/creative_fluid/cycle_medium"}],...}
```

After dropping read-only properties, the wrench decides from the count: none, it says the block
does not cycle; one, it cycles at once; two or more, it opens a menu whose buttons send
`/trigger ra.wrench set <row + 1>`.

The UNI gate's row is registered by the tools (`registerToolsWrenchCycles`) because its cycler,
`ra:tools/wrench/cycle_uni_gate`, lives in `ra:tools`.

!!! warning "Read before you write"
    A cycler that flips a value and then tests the same condition sees what it just wrote. That
    mistake cost the pack a jetpack toggle, a block breaker cooldown and a clock. Read the
    current value into a score or storage first, or `return` right after each write, as
    `cycle_uni_gate` does.

### Plain RMB: toggles

```kotlin
wrenchToggles += TagRoute(Emitter.block.tag, Emitter.toggle)
```

`ra:tools/wrench/found_block` runs each registered toggle as the marker whose tag matches, then
the multiblock-base assembly, then "This block cannot be toggled." for anything else. Only blocks
with one on/off state worth reaching for register one (the Wireless Emitter and Receiver).

## Read-only properties

```kotlin
readonly("electric_generator", "generation_rate")
readonly("multiblock_base", "tier", "tier_level")
```

Properties the block owns and a player must not type into: a generator's rate, a valve's
transfer rate. `ra:tools/readonly/init_registry` writes them to `storage ra:dh readonly` as
compounds (`{"electric_generator":{generation_rate:1b}}`), sorted by type, because both readers
ask "is *this* name read-only?", one command against a compound.

- The Data Handler still **shows** the row, with a struck-through `[Modify]` and a reason on hover.
- The wrench drops it from the cycle menu. A block left with nothing reports that it does not
  cycle, rather than cycling the one thing you locked.

## Data Handler property list

`ra:tools/data_handler/init_registry` (`tools/datahandler/DataHandlerRegistry.kt`) writes three
lists into `storage ra:dh`:

| List | Contents |
| --- | --- |
| `registry` | Every property name the Handler can show, in menu order |
| `numeric` | Names that hold numbers: the editor forces a number, and never writes a string |
| `item_names` | Names whose value is an item id (`filter_item`) |

The list is kept **centrally** in `DataHandlerRegistry.PROPERTIES`, in the beet order. A
module can add names with `ra.dataHandlerProperties("name")`; they go on the end.

!!! danger "Append only"
    A row's button carries `100 + its index`, and those ids are baked into chat messages
    players already have on screen. Inserting a name in the middle renumbers every row after
    it, so a menu drawn before a `/reload` edits the wrong property when clicked.

`channel`, `anchor_id`, `tag` and `entity_selector` must stay out of `numeric`: they are
identifiers blocks compare as text, and turning a channel into a number is the 5.1.6 bug that
stopped a vault matching its partner.

All three registries are **configuration, not world state**. `ra:load` rewrites them on every
load, unguarded. Guarding one on its own presence (`unless data storage ra:dh numeric`) is what
once left upgraded worlds without the filter rows for good.

## Tool items

Each tool item is one `ToolItem` (`tools/ToolItems.kt`): its id and an `ItemSpec.fromSnbt` with the
base item and an ordered list of components as SNBT. From that one value come the `give` argument, the recipe result
(`CustomComponent`s, see [Kore Gotchas](gotchas.md#item-components-in-json)) and the bundle
entry. Most tools are a food with an endless `consumable`: `using_item` is the only advancement
trigger that fires reliably on right-click whatever the player looks at, and only for an item
the player can start using.

`tools/ToolsDsl.kt` holds the writers every tool repeats; the `execute ... run function` lines use
the shared `executeRun` / `executeReturnRun` / `callWith` of `ra.core` (`core/Exec.kt`):

| Helper | Writes |
| --- | --- |
| `tool("wrench/menu")` | `ref(Ns.RA, "tools/wrench/menu")` |
| `raycast(steps, foundFlag, fn)` | one probe per step along the player's look, stopping once `fn` sets `foundFlag` |

The chain stays vanilla text because the typed `execute { }` builder writes these
`anchored eyes positioned ^ ^ ^2 unless data storage …` chains three times longer; the called
function is still a `FunctionArgument`, so `PackValidator` sees it.

## Give bundles

```kotlin
bundles += GiveBundle("gates", "Gates Bundle", "UNI Gate + timing utility blocks", ns, blocks.map { it.itemStackNbt() })
```

| Field | Becomes |
| --- | --- |
| `id` | `ra:items/bundles/give_<id>_bundle` |
| `title` | `item_name` |
| `lore` | one grey, non-italic lore line |
| `ns` | `custom_data={ra:{bundle_namespace:"<ns>"}}` |
| `items` | `bundle_contents` |

Hub (`hub/items/Bundles.kt`) writes one function per bundle and `give_all` calling them in
`HAND_OUT_ORDER` (tools, interactive, storage, gates, sensors, wireless, wires, chunk_loader,
multiblock, infinite, jetpacks, ender), the order the beet pack handed them out in. A bundle not
in that list follows in registration order.

`GiveBundle` cannot yet express a second lore line, a label that is not a namespace, or a
message after giving. `Bundles.kt` covers the three beet cases in local maps (`EXTRA_LORE` and
`NAMESPACE_LABEL` for `tools`, `AFTER_GIVE` for `ender`). Widening `GiveBundle` would remove
them.
