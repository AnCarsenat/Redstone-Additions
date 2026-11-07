# Developer Guide

This guide documents the runtime systems of the pack — the contracts each library and tool
keeps, and why they are built the way they are — and where each lives in the Kotlin source.

- Conceptual runtime flow first: [How It Works](how-it-works.md).
- Building, installing into a world, strict mode: [Getting Started](getting-started.md) and
  [Contributing Guidelines](contributing-guidelines.md#installing-into-a-world).
- How the Kotlin is organised: [Architecture](architecture.md) and
  [Project Reference](project-reference.md).

The pack is Kotlin in `redstone_additions_kore/src/main/kotlin/ra/`, turned into the data pack by
[Kore](https://kore.ayfri.com). Function paths below (`ra_lib:redstone/detect`) are what the game
runs; the Kotlin that writes `<ns>:<path>` sits in the package that mirrors it, and every public
library entry point has a typed reference in `ra/api/` (`RaLib.Redstone.detect`).

## 1) Core Entrypoints (`ra`)

Primary core functions:

- `ra:load`
- `ra:tick`
- `ra:give_all_items`
- `ra:uninstall`

`ra:give_all_items` now delegates to `ra:items/bundles/give_all`, which gives categorized prefilled bundles directly.

`minecraft:load` points to `ra:load`.

### `ra:load` responsibilities

- Initializes trigger and runtime scoreboards.
- Seeds shared storage in `ra:temp`.
- Initializes `ra_lib` and `ra_lib_multiblock`.
- Initializes all gameplay namespaces, including `ra_storage` and `ra_wires` transport networks.
- Starts main tick loop.

### `ra:tick` responsibilities

- Clears stale click tags.
- Runs modular input session processing and Data Handler action processing.
- Runs placement detection.
- Dispatches module ticks.
- Runs goggles scanning.
- Reschedules itself every tick.

## 2) Shared Library: `ra_lib`

`ra_lib` is the reusable systems layer.

### `ra_lib:init`

Calls module initializers:

- `orientation/init`
- `placement/init`
- `inventory/init`
- `redstone/init`
- `input/init`

### placement

Key functions:

- `ra_lib:placement/place`
- `ra_lib:placement/set_block`
- `ra_lib:placement/set_block_facing`
- `ra_lib:placement/set_block_simple`

Contract of `place`:

- Input macro fields: `block_id`, `block_tag`, `dir_type`.
- Resolves facing through orientation library.
- Places physical block.
- Summons marker with `ra.custom_block`, typed tag, and `ra.new`.
- Stores rotation/facing for downstream logic.

### orientation

Key functions:

- `ra_lib:orientation/get_facing`
- `ra_lib:orientation/set_facing`

`dir_type` behavior:

- `0`: no facing behavior
- `1`: horizontal facing only
- `2`: full directional (including up/down)

### redstone

Entry points, cheapest first:

- `ra_lib:redstone/any` — powered at all? Returns 1/0. Stops at the first live side.
- `ra_lib:redstone/detect_switch` — `any`, wrapped so it maintains the `ra.powered`
  tag. For blocks that use redstone as a switch. **Does not write `ra.power`.**
- `ra_lib:redstone/local/{front,back,left,right,up,down}` — one named side, 0-16,
  resolved through the block's own `ra.facing`.
- `ra_lib:redstone/side` — one compass side, 0-16. The macro core; everything
  above and below is built on it.
- `ra_lib:redstone/detect` — all six sides, the aggregate, and `ra.powered`.
- `ra_lib:redstone/detect_local` — `detect` plus the look-space scores and the
  direction tags. Only if you actually read them.
- `ra_lib:redstone/count_inputs` — how many sides carry a component, powered or
  not. Shares its source rules with `has_input`.

Cheaper than any of them: if the block's vanilla base already carries the answer,
read the block state and call nothing. `dispenser[triggered=true]`,
`note_block[powered=true]`, `redstone_lamp[lit=true]`. Note that `triggered` on a
dispenser or dropper also picks up quasi-connectivity, which a library scan does
not — that is a behaviour difference, not just a speed one.

Sources live in block tags, so adding one is a data edit rather than a code edit:

- `#ra_lib:redstone/binary_sources` — full power when `powered=true`: levers,
  buttons, non-weighted pressure plates, tripwire hooks, lightning rods.
- `#ra_lib:redstone/directional_sources` — strong power into the block they face:
  repeaters, comparators, observers.
- `#ra_lib:redstone/analog_omni` — carry a level in `power` and give it to every
  neighbour: both weighted pressure plates, daylight detectors.
- `#ra_lib:redstone/analog_sources` — the above plus redstone dust, which needs a
  connection test before its level counts.

It computes:

- aggregate power (`ra.power`, range `0..16`)
- world-space directional power (`ra.power.north/south/east/west/up/down`)
- look-space directional power (`ra.power.front/back/left/right/local_up/local_down`)
- power tags (`ra.powered`, directional tags, strong tag)

Power level contract:

- `0` = no power
- `1..15` = normal redstone power
- `16` = superpower (direct powered repeater/comparator output into the block)
- `ra.powered.strong` is set only when `ra.power == 16`

Consumer model:

- Gates and wireless emitter now consume `ra_lib:redstone/detect` directly per marker tick.
- Legacy gate signal batching (`ra_gates:check_signals`) remains as a compatibility no-op and is not used by runtime tick flow.

Source-specific detectors are split under `ra_lib:redstone/detect/*`.

### inventory

Key functions:

- `ra_lib:inventory/move_slot` — whole-slot transfer with `/item replace`
- `ra_lib:inventory/insert_or_drop` — insert what fits, drop the rest
- `ra_lib:inventory/insert` — raw `loot insert`
- `ra_lib:inventory/remove`
- `ra_lib:inventory/find_free_slot`, `has_free_slot`, `container_size`
- `ra_lib:inventory/clear`

!!! danger "`insert` destroys overflow"
    `loot insert` silently deletes whatever the destination cannot hold. Only use
    `insert` directly with a count of 1, where the item either fits or the call
    returns 0. For anything larger use `insert_or_drop`, which recovers the
    difference and drops it as an item entity.

`move_slot` is the preferred primitive for moving an existing stack: it copies
the stack verbatim, with no loot table to parse and no NBT arithmetic.
`remove` handles any container size and amounts split across stacks,
all-or-nothing.

### transport

Key functions:

- `ra_lib:transport/tick` — rebuilds networks when the topology changed
- `ra_lib:transport/net/join`, `rejoin`, `leave` — membership
- `ra_lib:transport/net/offer`, `take`, `read` — contents
- `ra_lib:transport/update_connection_status` — neighbour count for visuals

The engine groups adjacent nodes of the same class into networks by flood fill.
Contents belong to the network, not to individual nodes, so a pipe run costs
nothing per tick and transfer is order-independent. Rebuilds happen only on
placement or break, debounced to at most one every 5 ticks.

Classes (`fluid`, `item`, `electric`) never merge, so an item pipe and a fluid
pipe can share a block line without interacting.

#### Multi-medium networks

A network holds **several media at once**. Everything below exists to keep one
invariant true:

> `amount` is exactly the sum of `amounts`, `media` holds exactly one entry per
> key in `amounts`, and `medium` is `media[0].m` — absent when the network is
> empty.

Every bug this system has had was that invariant breaking somewhere.

**The shape.** One compound per network, at `storage ra:transport nets.n<id>`:

| Field | Example | What it is |
| --- | --- | --- |
| `amount` | `10000` | The total across every medium. Capacity is checked against **this**, so a run clogs on the sum and not on any one medium. |
| `capacity` | `20000` | Summed from `ra.tr.cap` on every node at rebuild. |
| `amounts` | `{water:5000,lava:5000}` | The per-medium breakdown. |
| `media` | `[{m:"water"},{m:"lava"}]` | The same keys as a list, in arrival order. |
| `medium` | `"water"` | A copy of `media[0].m`, because every display and every bridge already reads it. |
| `potion` | *(optional)* | A potion network's effect list. It belongs to the network, not to any node. |

`amounts` and `media` are the same data indexed twice, and both are needed. A
function cannot enumerate a compound's keys, so the walk has to go over a list;
lookups by name have to go through a compound. The list holds **compounds**
rather than bare strings so that a medium can be removed by value when it runs
out — `data remove storage ... media[{m:"water"}]` only works on compound
elements.

Totals live in storage rather than in scoreboards because a scoreboard gives one
number per network and a compound gave `amounts` a place to go without moving
anything that was already there. Arithmetic still goes through scoreboards,
because commands have no other way to add two numbers; storage is where values
live between operations, not where they are computed.

**Writing.** `net/offer` and `net/take` read the network, work out how much
actually moves, then hand the id and the medium to `net/offer_write` /
`net/take_write`, which are macro functions because the path contains the id and
the medium name.

- **Space is the sum.** An offer is refused only for lack of room. A network no
  longer turns away an unfamiliar medium — that is what made a run single-purpose
  for as long as it held anything.
- **A take is per medium and the name is required.** "Take 1000" has no answer on
  a mixed run. `net/take_any` takes the primary and parks its name in
  `storage ra:transport took.medium`, for callers like a Valve that move contents
  without caring what they are but must tell the far side what arrived.
- **First arrival.** `data get` on a path that is not there fails, a failed
  command stores 0, and 0 is the right starting value for a medium arriving for
  the first time. That is the test for "append to `media`".
- **Drained to nothing.** The key goes from `amounts`, the entry goes from
  `media`, and `medium` is recomputed from the new `media[0]` — removed first, so
  an emptied network is left with no medium at all rather than a stale one, since
  `set from` fails silently on an empty list and would leave the old value
  standing.

**The rebuild is where it gets hard.** Contents belong to the network, and a
rebuild discards every network, so the contents have to survive outside one:

1. `rebuild/snapshot` — each network's root marker parks the **whole breakdown**
   as `data.data.carry`, a list of `{m,a}`, and `ra.tr.carry` is summed from
   those entries rather than read off `amount`. Summing it is what keeps the
   total and the breakdown from disagreeing on the way back.
2. `nets` is wiped. Ids are reissued, old roots first, so a component containing
   an old root inherits its number and a retired number is never reissued.
3. `rebuild/accumulate_node` — capacity and carried total per new network.
4. `rebuild/absorb` — folds each carrier's `{m,a}` list back in, medium by
   medium, so two runs joined by a new pipe arrive as two breakdowns that add up.
5. `rebuild/clamp` — a network that lost a tank can be over capacity. The excess
   comes off the **newest** medium first, walked from `media[-1]`, which is the
   same rule the primary follows from the other end.

Two rules in there are load-bearing and were both learned the hard way:

- **Nothing travels that cannot be named.** A total with no breakdown behind it
  used to be carried across anyway, on the grounds that dropping it silently was
  worse. It is worse: the carried total has nothing to rebuild `amounts` from, so
  the next rebuild carries it again, and the next medium offered becomes the only
  entry in `media` and therefore the name of the whole thing.
- **A list element is built whole in storage, then appended.** The obvious
  version appends `{m:"water",a:0}` and fills the amount in with
  `execute store result entity @s data.data.carry[-1].a`. That silently does
  nothing. A write to entity data is applied by building a compound from the path
  and merging it, and there is nothing for a list *index* to merge into. Reading
  through an index is fine; only writing is not.

**Migration.** A network saved before multi-medium has `amount` and `medium` and
no breakdown. Ids cannot be enumerated from a function, so there is no sweep on
load: `net/migrate_pick` runs lazily, the first time such a network is read and
again at snapshot time, and moves the whole amount into the key of the medium it
was recorded as holding.

The guard is `unless data ... media[0]`, **not** `unless data ... media`.
`rebuild/reset_net` writes `media:[]`, and an empty list is present as far as
`if data` is concerned — so the shorter test never fires on the networks that
most need it.

**Reading and display.** `net/read` loads `cur.medium`, `cur.amounts` and
`cur.media` into `storage ra:transport`, plus `#net_amount` / `#net_capacity` as
the totals. It is **read-only**: it is called many times a tick, by every block's
tick and every goggles refresh, so anything that writes back from here mutates
live state at an arbitrary moment.

- The **Goggles** show the plain name for one medium and `Multimedium` for two or
  more. The test is `cur.media[1]` — cheaper than counting, and exactly "two or
  more". A billboard is one short line read from across the room.
- The **Multimeter** walks `cur.media` and prints a line per medium, in arrival
  order, because chat has room for it.
- A **bridge** moves `cur.medium` by default; a Liquid Filter substitutes its
  `filter_medium` and moves only that, which is what lets a mixed run be sorted
  back into single-medium branches.

### input

Key functions:

- `ra_lib:input/session/create`
- `ra_lib:input/router/select_backend`
- `ra_lib:input/router/open`
- `ra_lib:input/poll`
- `ra_lib:input/consume`
- `ra_lib:input/session/cleanup`

Backends:

- `trigger`: numeric input and range validation.
- `writable_book`: text capture from page 1.

Runtime behavior:

- Per-request state is stored under `storage ra:input` (`sessions.req_<id>`).
- `give_book_safe` only gives an Input Form when inventory has room.
- Full inventory produces a red user warning and skips book give.
- Dropped request books are cleaned through request-aware selectors.

## 2b) Settings: `ra_settings`

Values that are not a property of one placed block. Per-block properties already
have a home — the marker's `data.properties`, edited with the wrench or the Data
Handler. This is for the other two kinds: pack-wide balance, and per-player
preference.

### Two scopes, reached two ways

`global` lives in `storage ra:settings global` and is reached **only** through
`/function ra_settings:admin/...`, which needs permission level 2. Being a
function tree is also what makes it discoverable: `/function` autocompletes, and a
macro argument completes to nothing.

`user` is per-player and reached **only** through `/trigger`, which needs no
permission. The two never mix: the in-game menu contains nothing but user rows, so
there is no operator setting on screen for a player to click and be refused.

### Everything is generated

One Kotlin file per page in `ra/settings/pages/` (typed rows: key, label, scope, default,
bounds, step). The generator in `ra/settings/gen/` writes, at build time:

| Generated function | What it is |
|---|---|
| `ra_settings:pages` | the menu registry — **user rows only** |
| `ra_settings:defaults` | seeds any setting with no value, and creates user objectives |
| `ra_settings:sync` | per-tick seeding for players who have no score yet |
| `ra_settings:admin/**` | the whole operator function tree |
| `ra_settings:admin_actions` | index → function, for button dispatch |
| `ra_settings:admin_pages` | index → page, for the after-action redraw |
| `ra_settings:blockmap` | block → label + enable code, for the disabled list |
| `ra_settings:uninstall` | removes exactly what the system created |

Because the generator knows every key, bound and step as a literal, the emitted
tree contains **no macros at all** — none of the quoting fragility that shapes the
rest of the library applies to it.

### Reading a setting

```mcfunction
function ra_settings:get     {key:"welcome",default:1}
function ra_settings:prop    {block:"electric_generator",prop:"generation_rate",default:60}
function ra_settings:user    {obj:"ra.u.snd",default:1}
function ra_settings:enabled {block:"electric_furnace"}
```

`get` **requires** a default. It used to answer 0 for a missing key, and zero is a
real value — "off" for a flag, "disabled" for a gate — so a key that was missing
for any reason read as a deliberate no and switched features off. Callers pass the
value that keeps things working.

### Where block defaults are applied

`prop` rows are seeded into `data.properties` at **placement**
(`ra_settings:placement/seed`), not read live. `ra_lib:util/property` runs for
every consumer, bridge and drain on every tick and is deliberately four commands
with no sub-calls; a settings lookup does not belong there. Applying at placement
also stops a retune silently re-balancing a build. `[Apply to placed]` is the
explicit opt-out.

### Triggers

`/trigger` is the only control surface a player without permissions has, so every
button a non-operator can press goes through one. A `suggest_command` pointing at
a `/function` is useless to them.

An enabled trigger appears in everyone's `/trigger` completion whether it can do
anything or not, so they are handed out where usable and taken back where not.
**`scoreboard players reset` is what takes one back** — the enabled flag is stored
with the score.

Codes on `ra.settings.open`: `1` your preferences, `2` server settings, `3`
disabled blocks, `N+4` menu pages. New entry points take a spare code rather than
a new objective.

### Ordering constraint

A click that opens an input session must be handled **after** `ra_lib:input/tick`
in `ra:tick`. Handled before it, the session is scanned in the same tick it was
created, and the book backend is written on the assumption that a tick has passed.
This is why `ra_settings:input_tick` sits beside the Data Handler's collector
while `ra_settings:tick` (seeding only) runs early.

---

## 3) Shared Multiblock Library: `ra_lib_multiblock`

`ra_lib_multiblock` provides generic lifecycle management for all multiblock types.

Initialization:

- creates `ra.multiblock`, `ra.mb_timer`
- prepares `ra:multiblock` and temp storage branches

Lifecycle functions:

- `try_assemble`: entrypoint called by wrench assembly flow.
- `create_marker`: summons aligned multiblock marker.
- `setup_marker`: writes standardized marker data model.
- `validate_all`: periodic validation pass.
- `validate_single`: per-marker check.
- `disassemble`: teardown and effects.

Hook tags:

- `#ra_lib_multiblock:validate`
- `#ra_lib_multiblock:setup_type`
- `#ra_lib_multiblock:check_structure`
- `#ra_lib_multiblock:on_break`

Any new multiblock type should plug into these hook tags instead of bypassing the library.

## 4) Data and Marker Conventions

Use this schema for custom block and multiblock markers:

- `data.properties`: user-configurable fields.
- `data.data`: runtime mutable state.
- `data.status`: readable status for goggles/UX.

Tag conventions:

- `ra.custom_block`
- `ra.custom_block.<id>`
- `ra.multiblock`
- `ra.new`
- `ra.broken`

Keep IDs consistent across:

- give item custom_data
- placement tag
- marker typed tag
- recipe result custom_data
- on-break drop item data

### Minecraft versions

The pack targets **26.3 only** (data pack format 121). Kore writes every resource in the shape
26.3 reads, and `pack.mcmeta` declares `min_format` = `max_format` = 121, so there are no
overlays. The beet build covered 1.21.9 – 26.3 with a hand-written `overlay_102` and an
`overlay_121` generated by `tools/port_26_3.py`; both are gone. See
[Porting History](porting-history.md#what-changes).

## 5) Tooling Integration

### Wrench

- mode cycling for compatible blocks
- assembly/toggle interactions for multiblock bases and markers

### Creative Data Handler

- property discovery and editing, driven by one registry
- editor chosen from the value's actual type
- internal data/status inspection helpers

Every property row comes from `storage ra:dh registry`, a plain list of property
names set by `ra:tools/data_handler/init_registry`. For each name the block actually
has, `props/render` probes the value's type and draws the matching row:

| Type | Detected by | Editor |
| ---- | ----------- | ------ |
| string | `data modify … set string` accepts it | `[Modify]`, text input |
| list | `<name>[0]` exists | `[Edit list]`, text input pasted as SNBT |
| bool | the value matches `0b` or `1b` | `[Toggle]`, applied at once |
| number | none of the above | `[Modify]`, number input |

!!! warning "`data get` is not a type test"
    `data get` succeeds on a string — it returns the string's **length** — so
    "succeeded, therefore a number" classifies every string as a number. That shipped
    briefly and made the Handler offer a number editor for `channel`, writing ints
    that no string comparison could ever match. `set string` is the real test: it only
    accepts a string source.

A row's button carries `100 + its registry index`, so `run_action` and
`apply_pending` each need **one** branch for all properties rather than one per
name. Menu actions stay below 100.

**Adding a property to a block therefore means adding its name to
`init_registry` and nothing else.** Before this, each property needed a
hand-written `props/show_<name>` row plus a branch in `run_action` and another in
`apply_pending`, which is why blocks could display a property the Handler had no way
to change — a wire's `transfer_rate`, a tank's `tier`, an anchor's id.

A data pack cannot iterate the keys of a compound, which is the only reason the
registry list exists at all. Note also that `ra:dh` state is global: the Handler is
a single-player tool, as it always was.

### Data Handler

- non-OP-friendly property editing menu
- Shift+RMB target scan for nearby custom markers
- uses `ra_lib:input` backends for numeric and text property edits
- supports pending-edit cancel flow and menu refresh cycle

When adding new configurable properties, update CDH and Data Handler mappings/defaults.

## Block Skins

Some vanilla blocks carry behaviour you cannot switch off. A **dispenser fires
its own inventory on any rising redstone edge**, and so does a dropper. A custom
block that stores items in itself and sits anywhere near redstone will therefore
eject them, and no datapack logic can intercept it — there is no event to cancel.

The fix is to stop making mechanics and appearance the same decision:

- place the block whose **behaviour** you want (a barrel: same 27-slot inventory
  and GUI, no dispense)
- put the **appearance** back with a `block_display` laid over it

The Unboxer is the worked example. Its `input1` is `~ ~ ~` — it holds the crates
it is unboxing in its own inventory — so as a dispenser it threw them on the
floor. It is now a barrel wearing a dispenser skin.

### Using it

In Kotlin, through the typed references:

```kotlin
function(RaLib.Skin.apply, nbt { this["real"] = "minecraft:barrel"; this["skin"] = "minecraft:dispenser"; this["id"] = "unboxer" })
function(RaLib.Skin.clear, nbt { this["id"] = "unboxer" })
```

which generate:

```mcfunction
# on placement, and to repair a missing skin
function ra_lib:skin/apply {real:"minecraft:barrel",skin:"minecraft:dispenser",id:"unboxer"}

# in the break handler, before the marker is killed
function ra_lib:skin/clear {id:"unboxer"}
```

Use `ra_lib:skin/apply_static` when the skin block has no `facing` property.

Then repair a skin that went missing, once per tick, only for the blocks
actually lacking one:

```mcfunction
execute as @e[type=marker,tag=ra.custom_block.unboxer] at @s unless entity @e[type=block_display,tag=ra.skin.unboxer,distance=..0.9,limit=1] run function ra_storage:blocks/unboxer/refresh_display
```

Skins are tagged `ra.display`, `ra.skin` and `ra.skin.<id>`, so uninstall clears
them all with one selector.

### Why it holds together

- **Facing is read back off the real block**, not from stored state, so a block
  rotated by any means still gets a matching skin and a skin that drifts out of
  sync repairs itself.
- **Scale 1.004 with translation −0.002** encloses the real block without the two
  surfaces sharing a plane. Sitting exactly on `1.0` z-fights.
- **A `block_display` has no collision and no interaction box**, so the real block
  behind it still takes right-clicks, hopper insertion and comparator reads.

### What it does not hide

This swaps the *model*, not the block. Anything a player can observe other than
the model still comes from the real block:

| | Consequence |
|---|---|
| **GUI** | The Unboxer opens a **barrel's 27 slots**, not a dispenser's 3×3. This is the most visible tell. |
| **Sounds** | Opening, breaking and placing use the real block's sounds. |
| **Mining** | Hardness, tool and particles are the real block's. |
| **Block states** | A barrel's `open` animation is hidden under the skin. |
| **Other mods/packs** | Anything reading the world sees a barrel. |

So it is right when you want a block's *mechanics* minus one unwanted behaviour,
and wrong when you need the skinned block's interactions too.

### When to reach for it

Worth doing for a block that **stores items in its own inventory** and is backed
by a dispenser or dropper. In this pack that is the Item Pipe, the Item Mover and
the Boxer — all still unconverted.

Do **not** use it for Block Breaker, Block Placer or the Breeder: those read
`dispenser[triggered=true]` deliberately, so the vanilla trigger is the feature.

### Goggles

- collects markers in range of any goggles wearer once, then draws each one
- `ra:tools/goggles/draw_block` and `draw_multiblock` are pure routing
- refreshes in timed batches

Block-defined billboard contract:

Each block owns `blocks/<name>/goggles`, defined next to the block in its Kotlin file. It must:

1. publish its display name to `storage ra:temp block_name`
2. `execute if data storage ra:temp name_only run return 0`
3. write `storage ra:temp billboard` and call `ra:tools/goggles/billboard/handle_billboard`
4. emit its own status lines

```mcfunction
data modify storage ra:temp block_name set value "Liquid Tank"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/data_line {path:"medium",label:"Medium: ",color:"aqua",suffix:"",y:0.8}
```

In Kotlin, `gogglesHeader(block, nameY)` writes steps 1–3 and the line helpers
(`propLine`, `dataLine`, `textLine`, `stackReset`, `stacked*Line`, in
`ra/core/block/GogglesLines.kt`) write step 4. See
[Custom Blocks](custom-blocks.md#goggles-readout).

The `name_only` early return is what lets `ra:tools/block_name` reuse this
dispatch to resolve names for the Data Handlers, so a block's name is written
once. Billboard offsets are measured from the marker, which sits at the block
**centre** — a slab-height block wants `name_y` around `0.7`, not `1.0`.

#### Stacked lines

`prop_line`, `data_line` and `text_line` each take a hand-picked `y`. That is fine
for two or three lines and a trap for more: every block invents its own ladder, and
a block with one line too many draws it at `y:0.0`, inside itself, where nobody can
read it. Blocks with several lines should say where their ladder starts and let the
library count:

```mcfunction
function ra:tools/goggles/billboard/stack_reset {top:110,step:22}
function ra:tools/goggles/billboard/stacked_prop_line {path:"mode",label:"Mode: ",color:"light_purple",suffix:""}
function ra:tools/goggles/billboard/stacked_text_line {label:"Enabled: ",value:"yes",color:"green",suffix:""}
```

`top` and `step` are hundredths of a block — macro arguments are pasted as text, so
the arithmetic is done with scoreboards and written back as a double. Each stacked
line takes the current height and steps down. A block that forgets `stack_reset`
falls back to `top:80, step:20`. The Poppy Generator, with five lines, is the
worked example.
- Use this to keep low-information blocks clean while enabling richer overlays on data-heavy blocks.

`ra_wires` blocks declare their cyclable properties to the wrench:

- Add an entry to `ra:tools/wrench/init_registry`, keyed by the block's type —
  the same string `ra_lib:placement/place` writes into the marker's `data.type`.
- Each entry is `{label, prop, fn}`: the name shown in the menu, the property
  read for the current value, and the function that steps it.
- One entry and shift+RMB cycles it immediately; two or more and the wrench opens
  a menu. Nothing else changes.

### Read-only properties

A property the block owns and the player must not edit is registered with
`ra.readonly(type, props…)`, keyed by block type. The Tools part builds
`ra:tools/readonly/init_registry` from every registration:

```kotlin
ra.readonly("electric_generator", "generation_rate")
```

```mcfunction
data modify storage ra:dh readonly set value {"electric_generator":{generation_rate:1b}}
```

Both tools obey it from that one place: the Data Handler shows the row with a
struck-through `[Modify]` and a reason on hover, and the wrench drops it from the
cycle menu. A block left with no cyclable properties after filtering reports that
it does not cycle, rather than silently cycling the one thing you locked.

It is a compound rather than a list because both readers ask "is *this* name
read-only?" — one command against a compound, a walk against a list.

Cyclers run **as the marker, at the block**, and message `@a[distance=..10]` —
the wrench never touches the player, so anything addressed to a player-side tag
reaches nobody.

Read the current state before writing it. A cycler that flips a value and then
re-tests the same condition sees what it just wrote; that mistake has cost this
pack a jetpack toggle, a block breaker cooldown and a clock.

When adding new status fields, update goggles scan/status handlers.

## Migrations

A world saved by an older version is brought up to date by `ra_migrations`, run
from `ra:load` before anything else touches the world.

- One function per version step, named for the step it bridges:
  `ra_migrations:5.1.8-to-5.1.9`. They live in `ra/migrations/`.
- `ra_migrations:run` calls them oldest-first, from the `steps` list in
  `MigrationsPart.kt`. Add new ones to the end.
- **Every migration runs on every load**, so each must be safe to run twice. Fill
  in what a newer version expects; never overwrite or destroy state.

The names are `-to-` rather than `->` because a resource location path may only
contain `[a-z0-9_.-/]`, and a file the loader skips is a migration that silently
never runs.

They are also **identifiers, not the pack version**. A find-and-replace that
bumps the version across the repo must skip `ra/migrations/`, or the chain
renames itself to nonsense like `5.1.9-to-5.1.9`.

What has needed one so far: tagging every entity `ra` so `/kill @e[tag=ra]`
works, writing `data.type` onto markers so the wrench and read-only registries
have a key, dropping the `enabled` property, and clearing skins so they redraw
with a wider anti-z-fighting margin.

## 6) Contributor Workflow

The step-by-step checklists live in [Contributing Guidelines](contributing-guidelines.md):
[new block](contributing-guidelines.md#new-block-checklist) and
[multiblock](contributing-guidelines.md#multiblock-checklist). In short:

1. Describe the block once as a [`CustomBlock`](custom-blocks.md); its item, recipe result,
   drop, bundle entry, placement tag and goggles route all derive from it.
2. Write its placement handler, tick and break cleanup with the block helpers.
3. Register what tools need (wrench rows, read-only properties) on the `Ra` registry.
4. Add its settings row, advancement, docs and changelog entry.
5. Render its recipe picture (below).
6. Build strict, install with `./gradlew koreRun -Pworld`, and play it.

### Recipe pictures

`docs/images/recipes/{namespace}/{name}.png` are generated — full details in
[Recipe Renderer](recipe-renderer.md):

```bash
python3 redstone_additions/tools/recipe_render/render.py <recipe.json>
python3 redstone_additions/tools/recipe_render/render.py --all
```

!!! note
    The renderer still reads recipes from the beet source tree
    (`redstone_additions/src/data/*/recipe/`). A recipe added or changed only in Kotlin can be
    rendered by passing the generated file,
    `redstone_additions_kore/build/kore/redstone_additions/data/<ns>/recipe/<name>.json`.

The renderer reads the recipe the way the game does, so a result's
`minecraft:item_model` component is honoured. Ingredients carry no components, so
a disguised RA item used *as* an ingredient needs an entry in
`tools/recipe_render/overrides.json`.

### The Planet Minecraft description

`readme.bbcode` is generated from `readme.md`, not written by hand — full details
in [Markdown to BBCode](markdown-to-bbcode.md):

```bash
python3 redstone_additions/tools/md_to_bbcode.py readme.md \
    --base-url https://github.com/AnCarsenat/Redstone-Additions/raw/main/
```

PMC has no heading or table tag, so headings become sized bold text and tables are
flattened; relative links need the `--base-url` above to survive.

## 7) Validation and Debug

After changes, run this minimum test set:

1. `/reload` with log inspection.
2. Place each changed block once and verify marker tags.
3. Break changed blocks and verify no orphan markers.
4. Verify recipe output tags/custom_data.
6. Test Data Handler and CDH edit operations on changed blocks, including full-inventory text-input warning behavior.
6. Test goggles status rendering for changed blocks.
7. For multiblocks, test both assemble and disassemble paths.

Useful selectors:

- `@e[tag=ra.custom_block,distance=..20]`
- `@e[tag=ra.multiblock,distance=..40]`
- `@a[tag=ra.debug]`

## 8) Common Failure Modes

Several failures the beet pack listed here cannot happen any more: a block's ids are derived
from one `CustomBlock`, placement handlers and goggles routes come from `ra.block(...)`, and
strict mode fails the build on a call to an undefined function. What is left:

- Forgetting to remove one-time tags (for example `ra.new`) — use `finishPlacement(block)`.
- A property the Data Handlers should list but `DataHandlerRegistry` does not name.
- Multiblock setup data missing required fields in storage before assembly.
- Assuming numeric wireless channels; runtime channels are string values.
- A module-level collection named like a registry list (`blocks`) inside `Ra.register()`
  resolving to the registry — see [Kore Gotchas](gotchas.md).

---

Related pages:

- [How It Works](how-it-works.md)
- [Block Reference](item-reference.md)
- [Item Reference](item-reference.md)
- [Extension Examples](extension-examples.md)
