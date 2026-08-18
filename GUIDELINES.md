# Redstone Additions — Contributing Guidelines

**Version:** v5.1.14  
**Minecraft:** 1.21.9 – 26.2 (data pack formats 88 – 107)

---

## Table of Contents

1. [Naming Conventions](#naming-conventions)
2. [New Block Checklist](#new-block-checklist)
3. [Multiblock Checklist](#multiblock-checklist)
4. [Release Checklist](#release-checklist)
5. [File Templates](#file-templates)

---

## Naming Conventions

### Entity Tags
- **Format:** `ra.{category}.{block_name}`
- **Examples:**
  - `ra.custom_block` — All custom blocks
  - `ra.custom_block.block_breaker` — Specific block type
  - `ra.multiblock` — All multiblock markers
  - `ra.multiblock.blast_forge` — Specific multiblock type
  - `ra.billboard` — Goggle billboard entities
  - `ra.display` — All display entities (billboards, indicators)
  - `ra.spawned` — Newly spawned bats for placement
  - `ra.place.{block_name}` — Bat placement routing tag
  - `ra.broken` — Block marked for break cleanup
  - `ra.new` — Newly placed block (one-tick marker)

### Player Tags
- `ra.wrench_active` — Player is using wrench this tick
- `ra.cdh_active` — Player is using Creative Data Handler this tick
- `ra.goggles_active` — Player has goggles equipped/held
- `ra.wrench_clicked` / `ra.cdh_clicked` / `ra.remote_clicked` — Click tracking
- `ra.debug` — Player receives debug messages

### Scoreboards
- **Format:** `ra.{name}` (lowercase, dot-separated)
- **Core:** `ra.temp`, `ra.cooldown`, `ra.craft_id`
- **Orientation:** `ra.facing`, `ra.yaw`, `ra.pitch`
- **Redstone:**
  - aggregate: `ra.power` (0..16)
  - world-space: `ra.power.up/down/north/south/east/west`
  - look-space: `ra.power.front/back/left/right/local_up/local_down` (`detect_local` only)
  - convention: `0` no power, `1..15` normal power, `16` superpower (a repeater,
    comparator or observer driving the block)
  - pick the cheapest reader that answers your question: `redstone/any` or
    `redstone/detect_switch` for on/off, `redstone/local/{side}` for one face,
    `redstone/detect` only when you need every side. Cheaper than all of them is
    a base block that carries the answer itself — `dispenser[triggered=true]`,
    `note_block[powered=true]`, `redstone_lamp[lit=true]` — though `triggered`
    also picks up quasi-connectivity, which a library scan does not.
  - new power sources go in the `#ra_lib:redstone/*` block tags, never in a
    per-direction copy of a test
- **Gates:** use `ra_lib:redstone/detect` outputs (`ra.power*`) directly; no dedicated gate redstone objectives
- **Wireless:** `ra.pulse_timer`, `ra.remote.pending`, `ra.remote.slot`
- **Multiblock:** `ra.multiblock`, `ra.mb_timer`, `ra.heat`
- **Transport:** `ra.tr.net`, `ra.tr.cap`, `ra.tr.class`, `ra.tr.amount`, `ra.tr.capacity`
- **Inventory:** `ra.inv.count`
- **CDH:** `ra.edit_type`

### Data Storage
- **Format:** `ra:{namespace}`
- **Examples:**
  - `ra:multiblock` — Multiblock direction data and assembly staging
  - `ra:temp` — Temporary computation data (cleared between uses)
  - `ra:block` — Block-specific temporary data
  - `ra:cdh` — Creative Data Handler state

### Custom Data (Item Components)
- **Format:** `custom_data={ra:{block_name:1b}}`
- **Examples:**
  - `{ra:{block_breaker:1b}}` — Block Breaker item
  - `{ra:{wrench:1b}}` — Wrench tool
  - `{ra:{goggles:1b}}` — Goggles tool

### Entity Data Properties
- **Properties:** `data.properties.{key}` (configurable by CDH)
- **Internal data:** `data.data.{key}` (runtime state)
- **Status:** `data.status.{key}` (readable by goggles)
- **Structure:** `data.type`, `data.facing` (multiblocks)
- **IO:** `data.inputs`, `data.outputs`, `data.controls` (multiblocks)

### Function Path Conventions
- **Block functions:** `{namespace}:blocks/{block_name}/{action}`
- **Item functions:** `{namespace}:items/give_{block_name}` or `give_all`
- **Tool functions:** `ra:tools/{tool_name}/{action}`
- **Library functions:** `ra_lib:{module}/{action}`

---

## New Block Checklist

When adding a new custom block, create or update **all** of the following:

### Required Files
```
{namespace}/function/blocks/{name}/
├── give.mcfunction            # Give the block item to player
├── handle_placement.mcfunction # Detect bat tag, place block + armor stand
├── register_block.mcfunction   # Registration (debug tellraw)
├── tick.mcfunction             # Per-tick break detection + process dispatch
├── on_break.mcfunction         # Cleanup on block destruction
├── process.mcfunction          # Block-specific logic (if applicable)
└── goggles.mcfunction          # Display name + goggles status lines
```

### Required Registrations
- [ ] `{namespace}/function/blocks/{name}/` — All 5-6 standard files (above)
- [ ] `{namespace}/function/items/give_all.mcfunction` — Add give call
- [ ] `{namespace}/recipe/{name}.json` — Crafting recipe (bat_spawn_egg output)
- [ ] `ra_advancements/advancement/{category}/get_{name}.json` — Unlock advancement
- [ ] `ra/function/give_all_items.mcfunction` — Included via module give_all
- [ ] `{namespace}/function/load.mcfunction` — Add `register_block` call
- [ ] `{namespace}/function/tick.mcfunction` — Add tick dispatch
- [ ] `ra/tags/function/placement_handlers.json` — Add handle_placement entry
- [ ] `{namespace}/function/blocks/{name}/goggles.mcfunction` — Declare the block's
      display name and its goggles status lines
- [ ] `ra/function/tools/goggles/draw_block.mcfunction` — Add one dispatch line
- [ ] If the block stores items in its own inventory, do **not** back it with a
      dispenser or dropper — those fire their contents on any rising redstone
      edge. Place a barrel and restore the look with `ra_lib:skin/apply`; see
      "Block Skins" in the Developer Guide.
- [ ] `CHANGELOG.md` — Document the addition

### Item Component Template
```json
{
  "id": "minecraft:bat_spawn_egg",
  "count": 1,
  "components": {
    "minecraft:item_model": "minecraft:{visual_block}",
    "minecraft:item_name": "{Display Name}",
    "minecraft:custom_data": { "ra": { "{block_id}": true } },
    "minecraft:entity_data": {
      "id": "minecraft:bat",
      "Tags": ["ra.spawned", "ra.place.{block_id}"],
      "Silent": true,
      "NoAI": true,
      "Invulnerable": true
    }
  }
}
```

---

## Multiblock Checklist

See `redstone_additions/src/data/ra_lib_multiblock/README.md` for the full API.

When adding a new multiblock structure:

- [ ] Register the structure in `ra_multiblock/function/register_types.mcfunction`
  - Block offsets are written **once**, for a north-facing structure, relative
    to the base. The library rotates them into south/east/west.
  - Name the IO positions there too (`inputs`, `outputs`, `controls`).
  - Set `tier` so the wrench picks it up — no wrench file needs editing.
- [ ] Write `tick.mcfunction` — use the `ra_lib_multiblock:io/*` helpers to reach
      named IO blocks instead of branching on facing.
- [ ] Add a `tick_dispatch.mcfunction` selecting `ra.multiblock.{id}` and list it
      in `ra_lib_multiblock/tags/function/tick.json`.
- [ ] Optional: an `on_break` hook in `ra_lib_multiblock/tags/function/on_break.json`
      if disassembly needs cleanup beyond removing the marker.
- [ ] Add I/O data for Goggles display
  - `ra/function/tools/goggles/draw_multiblock.mcfunction` — Add draw entry
- [ ] Add advancement in `ra_advancements/`
- [ ] Update `CHANGELOG.md`

Assembly, per-facing rotation, periodic structure validation, disassembly and the
`ra.multiblock.{id}` marker tag are all handled by the library. Validation,
`check_structure` and `setup_type` hooks are only needed for a structure whose
shape cannot be expressed as a list of required blocks.

> Blast Forge and Upgrade Platform predate this and still carry hand-written
> per-facing coordinate tables and validators. They work; leave them unless you
> are deliberately migrating them.

---

## Release Checklist

Before releasing a new version:

- [ ] Bump the version everywhere: `pack.mcmeta`, `ra/function/load.mcfunction`,
      `readme.md`, `GUIDELINES.md`, `docs/index.md`, `docs/developer-guide.md`,
      `docs/contributing-guidelines.md`
- [ ] Update version in `pack.mcmeta` (description text)
- [ ] Update version in `ra/function/load.mcfunction` (load message + welcome message)
- [ ] Update README badge version (`readme.md`)
- [ ] Update WIKI `Home.md` version number
- [ ] Regenerate the Planet Minecraft description from the readme:
      `python3 tools/md_to_bbcode.py ../readme.md --base-url https://github.com/AnCarsenat/Redstone-Additions/raw/main/`
- [ ] Update `CHANGELOG.md` with all changes, categorized under Added/Changed/Fixed/Removed
- [ ] Run full test:
  - [ ] `/reload` — check for errors in game log
  - [ ] Place every custom block type
  - [ ] Verify all crafting recipes work
  - [ ] Test wrench on UNI Gate (cycle modes) and emitter/receiver (toggle)
  - [ ] Test Creative Data Handler on each block type
  - [ ] Test goggles scanning
  - [ ] Assemble and test Blast Forge multiblock
  - [ ] Test uninstall flow (confirm + cancel)
- [ ] Remove all debug/test items and functions
- [ ] Verify no broken function calls (`/reload` and check logs)
- [ ] Run `/function ra:give_all_items` and verify all items are given

---

## File Templates

### give.mcfunction
```mcfunction
# /{namespace}:blocks/{name}/give
# Give {Display Name} block item to player

give @s bat_spawn_egg[item_model="minecraft:{visual_block}",item_name="{Display Name}",custom_data={ra:{{block_id}:1b}},entity_data={id:"minecraft:bat",Tags:["ra.spawned","ra.place.{block_id}"],Silent:1b,NoAI:1b,Invulnerable:1b}]
```

### handle_placement.mcfunction
```mcfunction
# /{namespace}:blocks/{name}/handle_placement
# Handle {Display Name} placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.{block_id}] run return 0

function ra_lib:placement/place {block_id:"minecraft:{physical_block}",block_tag:"{block_id}",dir_type:0}

tag @s remove ra.place.{block_id}
return 1
```

### register_block.mcfunction
```mcfunction
# /{namespace}:blocks/{name}/register_block
# Register {Display Name} block type

tellraw @a[tag=ra.debug] [{"text":"[RA] ","color":"gold"},{"text":"{Display Name} registered","color":"gray"}]
```

### tick.mcfunction (per-block)
```mcfunction
# /{namespace}:blocks/{name}/tick
# Tick all {Display Name} blocks

# Break detection
execute as @e[tag=ra.custom_block.{block_id}] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.{block_id}] at @s run function {namespace}:blocks/{name}/on_break
tag @e[tag=ra.broken,tag=ra.custom_block.{block_id}] remove ra.broken

# Process logic
execute as @e[tag=ra.custom_block.{block_id}] at @s run function {namespace}:blocks/{name}/process
```

### on_break.mcfunction
```mcfunction
# /{namespace}:blocks/{name}/on_break
# Handle {Display Name} break

# Kill vanilla block drop
kill @e[type=item,nbt={Item:{id:"minecraft:{physical_block}"}},distance=..2,limit=1,sort=nearest]

# Drop custom item
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:{visual_block}","minecraft:item_name":"{Display Name}","minecraft:custom_data":{ra:{{block_id}:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.{block_id}"],Silent:1b,NoAI:1b,Invulnerable:1b}}},Tags:["ra.drop_temp"]}

# Kill marker
kill @s
```

---

## Architecture Overview

```
ra              — Core: placement system, tools (wrench, CDH, goggles), crafting
ra_lib          — Shared library: orientation, redstone detection, inventory, placement
ra_lib_multiblock — Multiblock library: assembly, validation, disassembly
ra_gates        — Logic Gates addon (UNI Gate, Clock, Delayer, Extender, Shortener, Randomizer)
ra_interactive  — Interactive Machines addon (Block Breaker, Placer, Pipe, Spitter, etc.)
ra_sensors      — Sensors addon (Entity Detector, Tag Adder, Tag Remover)
ra_wireless     — Wireless Redstone addon (Emitter, Receiver, Remote)
ra_multiblock   — Multiblock implementations (Blast Forge + bases)
ra_advancements — Advancement trees for all addons
```

Each addon is independent and hooks into the core via:
- **Function tags** (`#ra:placement_handlers`) for placement routing
- **Direct calls** to `ra_lib:` for shared utilities
- **Tick dispatch** from `ra:tick` → `{module}:tick`
- **Load dispatch** from `ra:load` → `{module}:load`

