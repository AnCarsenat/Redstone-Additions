# Tools and Registries

The tools (`ra.tools`, namespace `ra`) read and edit blocks they know nothing about at compile
time. They learn about blocks through the [`Ra` registry](architecture.md#the-registry): a block
registers what it offers, and `ToolsPart`, which runs after every module, builds the shared
functions from what was registered. This page is the contract on both sides. For what each tool
does in game, see [Tools](tools.md).

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

A billboard is one `text_display` (plus an `item_display` for an item line), built as a list
of text components in `storage ra:temp gg.text` with `{text:"\n"}` between lines. A text
display lays its lines out upward from its position, so the billboard keeps its base and grows
with its content: a line is 10 px (9 of glyph + 1 of spacing) × 0.025 block × the 0.4 scale =
0.1 block.

Billboard writers callable from a readout:

| Reference | Path | For |
| --- | --- | --- |
| `Goggles.Billboard.*` (`ra/api/ToolsApi.kt`) | `ra:tools/goggles/billboard/{handle_billboard, prop_line, data_line, text_line, item_line, show_io_line, stack_reset, stacked_*_line, init_offsets}` | block readouts; wrapped by `core/block/GogglesLines.kt` |
| `Goggles.Billboard.*` (same object) | `handle_multiblock`, `show_input`, `show_output`, `show_control`, `show_literal_line`, `render_*` | multiblock readouts and the tools' own rendering |
| `Goggles.Billboard.{begin, append, flushHere}` | `begin`, `append`, `flush_here` (+ `spawn_text`, `spawn_item`, `load_schema`) | the billboard buffer: `draw_block` / `draw_multiblock` call `begin`; every writer puts its line in `storage ra:temp gg.line` and calls `append`; `draw_block` calls `flush_here` 0.3 above the block, a multiblock's own function calls it at its structure's height |
| `Goggles.Status.readStatusMultiblock` | `ra:tools/goggles/status/read_status_multiblock` | multiblock status |

## Wrench

### Shift+RMB: cycling

A block declares the properties the wrench may cycle, keyed by its marker's `data.type`:

```kotlin
// ra/modules/wires/WiresBlocks.kt
wrenchCycle("electric_consumer", WrenchCycle("Draw", "eu_use", ElectricConsumer.cycleRate))
wrenchCycle("electric_furnace",
	WrenchCycle("Output", "output", ElectricFurnaceSettings.cycleOutput),
	WrenchCycle("Power", "mode", ElectricFurnaceSettings.cycleMode))
```

| Field | Meaning |
| --- | --- |
| `label` | Row name in the menu |
| `prop` | `data.properties.<prop>`, shown as the current value. Must be the property `fn` changes, declared and not `readonly` in the block's schema: the build fails otherwise |
| `fn` | The cycler. Runs **as the marker, at the block**; messages `@a[distance=..10]` |

`ra:tools/wrench/init_registry` (`tools/wrench/WrenchRegistry.kt`) writes every row into
`storage ra:wrench cyclables`, keys sorted, on every load:

```mcfunction
data modify storage ra:wrench cyclables set value {"creative_fluid":[{label:"Medium",prop:"medium",fn:"ra_wires:blocks/creative_fluid/cycle_medium"}],...}
```

After dropping what the player may not change (by the block's
[property schema](#property-schemas): read-only rows always, admin-only and hidden rows unless the
player holds `ra.admin`), the wrench decides from the count: none, it says the block
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
wrenchToggles += TagRoute(MyBlock.block.tag, MyBlock.toggle)
```

`ra:tools/wrench/found_block` runs each registered toggle as the marker whose tag matches, then
the multiblock-base assembly, then "This block cannot be toggled." for anything else. No shipped
block registers one: the Wireless Emitter and Receiver were the last, until v6.0.1 removed their
hidden `enabled` switch. The registry stays for extensions.

## Property schemas

Every block type declares its properties next to its definition, in declaration order — which
is the Data Handler's menu order:

```kotlin
// ra/modules/ender/TeleportAnchor.kt
block(block)
properties(block) {
	text("anchor_id")
	list("targets", of = PropType.TEXT, max = 15, pad = true)
}

// ra/modules/wires/WiresBlocks.kt -- keyed by the marker type, which is not the item id here
properties("liquid_drain") {
	number("speed", min = 1, max = 100, adminOnly = true, goggles = false)
	// Pre-6.0.1 names, read only by the drain's migration, which removes them.
	number("cooldown", readonly = true, visible = false)
	number("interval", readonly = true, visible = false)
	text("mode", readonly = true, visible = false)
}
properties("electric_generator") { number("generation_rate", min = 1, max = 10000, readonly = true) }
```

`properties(block)` keys the schema by `block.id`; `properties("type")` by any `data.type`
(Wires markers, `multiblock_base.<tier>`, multiblock structures, which `Ra.multiblock(spec)`
declares for you). `ra/core/Properties.kt` holds the builder.

| Declaration | Type | Editor in the Data Handler |
| --- | --- | --- |
| `number(name, min = 0, max = 9999)` | int | number prompt, bounded |
| `bool(name)` | byte `0b`/`1b` | **[Toggle]** |
| `text(name)` | string | the book; stored as typed, never parsed |
| `item(name)` | string (item id) | the book, or **[Set from hand]** |
| `text(name, TextFormat.SELECTOR)` | string in a [format](#text-formats) | the book, checked against the format |
| `list(name, of = PropType.TEXT, max = 15, pad = true)` | list | one row per entry ([Edit], [Remove]/[Clear], [Append]) and **[Edit whole list]** |
| `textList(name, max, pad, format, choices = "ra:ender" to "anchor_ids", entryLabel = "Strength ")` | list of text | the same, with **[Choose]** from `storage <choices>` and entries labelled *Strength 1…* |
| `dynamic(name)` | read from the block instance | — (see below) |

Each also takes these flags:

| Flag | Data Handler | Wrench | Clipboard paste |
| --- | --- | --- | --- |
| `readonly = true` | shown, locked, reason on hover; writes refused | never cycled | not pasted (the target keeps its own) |
| `adminOnly = true` | locked for players without `ra.admin` (server-settings access), editable for admins; re-checked when the value is written | not offered to non-admins | not pasted by non-admins |
| `adminOnly = true, adminSetting = "<key>"` | as `adminOnly`, unless the GLOBAL bool setting `<key>` is on (then everyone may edit it) | as `adminOnly` | as the Data Handler |
| `visible = false` | not shown at all to non-admins (nor in the Properties summary); admins see it marked *(hidden from players)* and can edit it | not offered to non-admins | not pasted by non-admins |

One more flag concerns only the Goggles:

| Flag | Goggles label |
| --- | --- |
| unset (default) | follows `visible`: a property hidden from the Data Handler is hidden from the label too |
| `goggles = true` | shown even when `visible = false` — for a value players should see but not edit or browse |
| `goggles = false` | never shown, though visible in the Data Handler — for fixed or uninteresting values such as a generator's `cooldown` |

The flag gates a property line the block's goggles function draws (`propLine` /
`stackedPropLine`); it does not add one. Status and data lines (`dataLine`, `stacked*Line`
reading `data.status` / `data.data`) are the block's own readout and are not gated. The label
is the same for every wearer, admins included, since one display serves everyone in range.

The Creative Data Handler shows and edits everything regardless; it is an operator tool.

### What is generated

`ra:tools/data_handler/init_registry` (`tools/datahandler/DataHandlerRegistry.kt`) runs on every
load, unguarded — configuration, not world state:

```mcfunction
data modify storage ra:dh types."liquid_drain" set value {rate:{type:"number",code:1,min:100,max:100000,readonly:0b,admin_only:1b,visible:1b,goggles:0b,i:0},speed:{type:"number",code:1,min:0,max:9999,readonly:1b,admin_only:0b,visible:0b,goggles:0b,i:1},cooldown:{type:"number",code:1,min:0,max:9999,readonly:1b,admin_only:0b,visible:0b,goggles:0b,i:2},interval:{type:"number",code:1,min:0,max:9999,readonly:1b,admin_only:0b,visible:0b,goggles:0b,i:3},mode:{type:"text",code:0,readonly:1b,admin_only:0b,visible:0b,fmt:1,goggles:0b,i:4}}
data modify storage ra:dh order."liquid_drain" set value ["rate","speed","cooldown","interval","mode"]
data modify storage ra:dh known set value ["anchor_id","chance","channel",...]
```

- `types."<type>".<name>`: the declaration. `code` is the type as a number and the flags are
  bytes, so a reader takes each with one `data get` into a score; "what is `<name>` on this
  block?" is one compound lookup, and nothing walks a list per tick.
- `goggles` is the resolved Goggles flag (`goggles ?: visible`), so the Goggles decide a line
  with one match on `types."<type>".<name>` — the billboard copies the block's type compound
  once per redraw, and each property line then tests it in storage. It is not written for a
  `dynamic` property: the Goggles then read `goggles` (or failing that `visible`) from the
  instance's `data.property_types.<name>`, and show the line when neither says otherwise.
- `order."<type>"`: the names in menu order.
- `known`: every name any type declares, which the Data Handler uses to give a row to a property
  a block carries without declaring.

Each type is written on its own line, so an add-on's types in the same storage survive the load
(see [Extension Examples](extension-examples.md)). The old `storage ra:dh readonly`, `registry`,
`numeric` and `item_names` are removed on load.

### Text formats

Several blocks paste a text property straight into a command through a macro (a sensor's
`entity_selector` into `execute as $(entity_selector) …`, a Boxer's `input1` into
`execute positioned $(input1) …`, a vault's `channel` into `{channel:"$(channel)"}`). A macro
substitutes text, so a value with a space or a quote could add arguments: `@s if function
<anything> run say` would run any function with the server's permission. Every text property
therefore has a format (`fmt` in the schema, `ra/core/PropFormats.kt`), checked by
`ra_lib:safe/check` when the Data Handler writes it and by `ra_lib:safe/guard` before each use:

| Format | Accepts | Used by |
| --- | --- | --- |
| `FREE` (0) | anything | Message Block text (never pasted) |
| `ID` (1, default for text, items, list entries) | letters, digits, `_ . + - : / #`; no space | most text properties |
| `NAME` (2) | `ID` plus spaces | channels, anchor names and targets |
| `TAG` (3) | letters, digits, `_ . + -`; not starting with `ra.` | sensor `tag` |
| `SELECTOR` (4) | `@a/@e/@n/@p/@r/@s` and one optional `[...]`: spaces only inside the brackets, `"..."` strings, no `\`, nothing after `]` | `entity_selector` |
| `COORDS` (5) | three `~`/`^` coordinates, at most one whole digit each | Boxer, Unboxer `input1`/`output1` |

The checker walks the string one character at a time. `"`, `\` and line breaks are matched
with plain NBT tests first; every other character is looked up as a quoted key in a class table
(`storage ra:safe cls`), the one macro line, safe because those three were removed before it.
`guard {p, f}` (as the block's marker) caches the last value found safe in `data.data.safe.<p>`
and the last refused one in `data.data.unsafe.<p>`, so an unchanged value costs a comparison.
In Kotlin, `guardProperty("entity_selector", TextFormat.SELECTOR)` before the macro call.

The checks apply to admins too: a value that needs more than its format can carry more than one
argument, which no block ever needs. Admin-only status decides *who* may choose what a block
targets; the format decides that what they write is only ever that one argument.

### Dynamic properties (discouraged)

`dynamic(name)` declares that each block instance carries its own declaration, in
`data.property_types.<name>` on its marker, in the same shape as above
(`{type:"number",code:1,min:0,max:10}`). The Data Handler and the wrench read it from the block.
Every dynamic declaration prints a build warning: the type then lives in world data, where no
build checks it.

### Undeclared properties

A property a block carries without a declaration (added with the Creative Data Handler, or by an
add-on that declares nothing) is never trusted to have a type. When some other type declares the
same name, the Data Handler gives it a row with a yellow **⚠** whose hover names the guessed type,
and players with the `ra.debug` tag get a line naming the block and the property. Anything else
is listed under *Undeclared*, with no editor. The guess comes from the value, by tests that only
one type passes (no macro needed, value first copied to `storage ra:dh probe`):

| Type | Test |
| --- | --- |
| list | `data modify storage ra:dh probe insert 0 value 0b` succeeds — only a list (empty or not) takes an insert |
| compound | `execute if data storage ra:dh probe{}` (no editor; use the Creative Data Handler) |
| number | `execute store result score #dh.n0 ra.temp run compute default integer {type:"minecraft:storage",storage:"ra:dh",path:"probe",fallback:0}` and the same with `fallback:1`: the storage provider returns its fallback when the value is absent or not a number, so the two agree exactly when it is one |
| bool | a number that is the byte `0b` or `1b`: `execute if data storage ra:dh {probe:1b}` (an NBT match is typed, so the int `1` does not pass) |
| text | anything else |

A declared type always wins over the value: a `number` whose value became the string `"5"` is
still edited as a number. `data modify … set string` is **not** a string test — it accepts a
number and writes its digits (see [Kore Gotchas](gotchas.md#set-string-accepts-a-number)).

### Typed text

Whatever the player types reaches a command only through `ra:tools/data_handler/props/parse`,
whose single line is `$data modify storage ra:dh parsed set value $(text)`: malformed text fails
that one call and leaves `parsed` absent. The parsed value is then type-tested like any other,
and the write uses the property's type at the moment of writing (declared, or guessed from the
value then):

| Type | Accepted | Written |
| --- | --- | --- |
| text, item | anything | `set from storage ra:input consume.text` — never parsed, so `5` typed for a text property stays the text `5` |
| number (declared) | the number prompt, within `min`..`max` (checked again on write) | the int |
| number (guessed) | typed text that parses to a number (`5`, `-3`, `2.5`) | as parsed |
| bool | `true`, `false`, `on`, `off`, `1`, `0` (also `True`, `TRUE`, …) — compared, not parsed | `1b` / `0b` |
| list | a list; text entries when declared `of = TEXT` (or, undeclared, when it holds text now); at most `max`, padded with `""` when `pad` | the list |

Anything else is refused with a red message and the block is left unchanged.

### Menu buttons

A row's button runs `/trigger ra.dh.action set <stamp × 10000 + code>`: code `100 + i` edits row
`i` of `order."<type>"`, `400 + i` is that row's **[Set from hand]**, `550 + i` its **[Clear]**
(item rows), `700 + i` edits the
undeclared property `known[i]`. A declared list's entries (rows 0–29, entries 0–98) use
`1000 + 300 i + j` (**[Edit]** entry `j`), `+ 99` (**[Append]**), `+ 100 + j` (**[Remove]** /
**[Clear]**) and `+ 200 + j` (**[Choose]**). A picker is a menu of its own (`ra.dh.mkind` 1)
whose choices are kept, as drawn, in `storage ra:dh pick.s<stamp>`; its buttons are
`100 + k`. An entry edit is filed under the player's input request with its index
(`pend.r<id>.j`, `-1` for an append). The stamp numbers the menu (1 – 200,000); each player's
latest is in `ra.dh.menu`, and loading resets it for everyone. A click carrying any other stamp — an old menu
scrolled back to, or one drawn before a `/reload` that may have reordered a schema — redraws the
menu instead, so schemas can be reordered freely.

### Where each piece of a property lives

| What | Where |
| --- | --- |
| Its value | the block's marker, `data.properties.<name>` |
| Its type, bounds, flags | the block type's `properties { }` in Kotlin, generated into `storage ra:dh types."<type>".<name>` on load |
| Menu order | the same declaration order, in `storage ra:dh order."<type>"` |
| A dynamic property's type and flags | the marker, `data.property_types.<name>` |
| A server-wide default | the settings `PropRow`, merged in at placement from `storage ra:settings global.props."<type>"` |
| Who is an admin | the `ra.admin` player tag (server-settings access) |

`channel`, `anchor_id`, `tag` and `entity_selector` are declared `text` on purpose: they are
identifiers blocks compare as text, and turning a channel into a number is the 5.1.6 bug that
stopped a vault matching its partner.

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
| `raycast(foundFlag, fn, guard, reach)` | walks the player's look in 0.1-block steps (5 blocks) and runs `fn` as the custom block whose block the look enters first, at its centre; a non-replaceable block with no marker ends it. One generated walker per `fn`, `ra:tools/ray/<fn path>` |

The chain stays vanilla text because the typed `execute { }` builder writes these
`anchored eyes positioned ^ ^ ^2 unless data storage …` chains three times longer; the called
function is still a `FunctionArgument`, so `PackValidator` sees it.

## Give bundles

```kotlin
bundles += GiveBundle("gates", "Gates Bundle", "UNI Gate + timing utility blocks", ns, gateBlocks.map { it.itemStackNbt() })
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
