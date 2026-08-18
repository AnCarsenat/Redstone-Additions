# Developer Guide

This guide documents implementation architecture and contributor workflow for `v5.1.13`.

If you want conceptual runtime flow first, start with [How It Works](how-it-works.md). This page is focused on engineering-level extension and maintenance work.

If you want to contribute and write somewhat correct addons / fixes see [Contributing Guidelines](contributing-guidelines.md)

## Development Setup

This project uses **[Beet](https://github.com/mcbeet/beet)** to build the datapack from source.

### Prerequisites
- Python 3.10 or higher
- Git

### Quick Start

#### 1. Clone the Repository
```bash
git clone https://github.com/AnCarsenat/Redstone-Additions.git
cd Redstone-Additions
git checkout beet_rewrite
```

#### 2. Set Up the Virtual Environment
Navigate to the `redstone_additions/` directory and create a Python virtual environment:

```bash
cd redstone_additions
python3 -m venv .venv
```

#### 3. Activate the Virtual Environment
**On Linux/macOS:**
```bash
source .venv/bin/activate
```

**On Windows (CMD):**
```bash
.venv\Scripts\activate.bat
```

**On Windows (PowerShell):**
```bash
.venv\Scripts\Activate.ps1
```

#### 4. Install Dependencies
```bash
pip install -r requirements.txt
```

#### 5. Link to Your Minecraft World
Use beet's `link` command to connect to your Minecraft world. Specify the path to your Minecraft installation and the world name:

```bash
beet link --minecraft /path/to/minecraft <world_name>
```

**Example with PrismLauncher:**
```bash
beet link --minecraft "~/.local/share/PrismLauncher/instances/26.2 Fabric/minecraft" DevWorld
```

#### 6. Build and Test
Build the datapack from source:

```bash
beet build
```

To enable auto-rebuild when you make changes, use watch mode:

```bash
beet watch
```

Then use `/reload` in-game to apply changes immediately.

### Development Workflow

1. Navigate to the `redstone_additions/` directory
2. Ensure the virtual environment is activated: `source .venv/bin/activate`
3. Edit files in the `src/` directory
4. Use `beet watch` for automatic rebuilding on file changes
5. Use `/reload` in-game to apply changes

---

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

### Pack format overlays

`src/pack.mcmeta` declares `min_format` 88 through `max_format` 107, i.e.
Minecraft 1.21.9 through 26.2. When a vanilla schema changes shape inside that
span, the fix is an **overlay**, not a version bump: an overlay directory sits
next to `data/` in `src/`, mirrors the same paths, and the game swaps its files
in for the format range the overlay declares.

There is one today:

```
src/
  pack.mcmeta                                  min_format 88, max_format 107
  data/ra/predicate/is_sneaking.json           {"flags": {"is_sneaking": true}}
  overlay_102/
    data/ra/predicate/is_sneaking.json         {"minecraft:flags": {"is_sneaking": true}}
```

```json
"overlays": {
  "entries": [
    { "directory": "overlay_102", "min_format": 102, "max_format": 107 }
  ]
}
```

Format 102 (`26.2-snapshot-3`) rewrote entity predicates into component-map
form. The base file keeps the pre-102 spelling and the overlay carries the new
one; the game picks whichever matches the running version, so both work from a
single build. beet copies overlay directories through untouched — no plugin, no
extra config.

Reach for this when a vanilla format changes under a file the pack already
ships. Do **not** reach for it for mcfunction differences: an overlay replaces
whole files, so duplicating a function into an overlay means maintaining two
copies of live logic forever. If a *command* changes across the range, prefer
narrowing the declared range over forking the function.

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

Each block owns `blocks/<name>/goggles.mcfunction`. It must:

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

A property the block owns and the player must not edit goes in
`ra:tools/readonly/init_registry`, keyed by block type:

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
  `ra_migrations:5.1.8-to-5.1.9`.
- `ra_migrations:run` calls them oldest-first. Add new ones to the end.
- **Every migration runs on every load**, so each must be safe to run twice. Fill
  in what a newer version expects; never overwrite or destroy state.

The names are `-to-` rather than `->` because a resource location path may only
contain `[a-z0-9_.-/]`, and a file the loader skips is a migration that silently
never runs.

They are also **identifiers, not the pack version**. A find-and-replace that
bumps the version across the repo must skip `ra_migrations/`, or the chain
renames itself to nonsense like `5.1.9-to-5.1.9`.

What has needed one so far: tagging every entity `ra` so `/kill @e[tag=ra]`
works, writing `data.type` onto markers so the wrench and read-only registries
have a key, dropping the `enabled` property, and clearing skins so they redraw
with a wider anti-z-fighting margin.

## 6) Contributor Workflow

Use this sequence for safe feature delivery.

1. Define block/multiblock ID and naming.
2. Implement give item, placement handler, tick, and break cleanup.
3. Register placement handler and namespace load/tick hooks.
4. Add recipe and advancement unlock path.
5. Add CDH property support for editable settings.
6. Add goggles status support for visible diagnostics.
7. Update docs and changelog.
8. Render the recipe picture (see below) and reference it from the module page.
9. Run in-world validation pass.

### Recipe pictures

`docs/images/recipes/{namespace}/{name}.png` used to be a screenshot per recipe.
They are generated now — full details in [Recipe Renderer](recipe-renderer.md):

```bash
python3 tools/recipe_render/render.py src/data/<namespace>/recipe/<name>.json
python3 tools/recipe_render/render.py --all          # the whole pack
```

The renderer reads the recipe the way the game does, so a result's
`minecraft:item_model` component is honoured. Ingredients carry no components, so
a disguised RA item used *as* an ingredient needs an entry in
`tools/recipe_render/overrides.json`.

### The Planet Minecraft description

`readme.bbcode` is generated from `readme.md`, not written by hand — full details
in [Markdown to BBCode](markdown-to-bbcode.md):

```bash
python3 tools/md_to_bbcode.py ../readme.md \
    --base-url https://github.com/AnCarsenat/Redstone-Additions/raw/main/
```

PMC has no heading or table tag, so headings become sized bold text and tables are
flattened; relative links need the `--base-url` above to survive.

### New Block Checklist (Practical)

1. Item custom_data and `ra.place.*` tags are correct.
2. Placement handler returns `1` only for matching bats.
3. Block tick includes break detection and cleanup.
4. On-break drop reproduces the same item signature.
5. Block appears in module `give_all` and in the correct namespace bundle from `ra:give_all_items`.
6. Recipe and advancement IDs align.

### New Multiblock Checklist (Practical)

1. Validation hook registered in `#ra_lib_multiblock:validate`.
2. Setup hook registered in `#ra_lib_multiblock:setup_type`.
3. Periodic check hook registered in `#ra_lib_multiblock:check_structure`.
4. Cleanup hook registered in `#ra_lib_multiblock:on_break`.
5. Wrench assembly flow can stage required data in `storage ra:multiblock`.
6. Marker stores `type`, `facing`, `properties`, IO/control metadata.

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

- ID mismatch between recipe result and placement handler tag.
- Missing placement handler registration.
- Forgetting to remove one-time tags (for example `ra.new`).
- Not updating CDH/goggles when adding new properties.
- Not updating Data Handler input mapping when adding editable properties.
- Multiblock setup data missing required fields in storage before assembly.
- Assuming numeric wireless channels; runtime channels are string values.

---

Related pages:

- [How It Works](how-it-works.md)
- [Block Reference](item-reference.md)
- [Item Reference](item-reference.md)
- [Extension Examples](extension-examples.md)
