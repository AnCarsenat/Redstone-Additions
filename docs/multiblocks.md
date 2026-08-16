# Multiblocks

This page documents the multiblock stack currently shipped in source:

- Runtime library: `ra_lib_multiblock`
- Implementations: `ra_multiblock`
- Full architecture: [How It Works](how-it-works.md)

## Base Tiers

`ra_multiblock:blocks/give_all` gives these base items:

- copper_base
- iron_base
- gold_base
- diamond_base
- netherite_base

Each base has a dedicated recipe in `data/ra_multiblock/recipe`.

## Lifecycle

1. Place a base block item.
2. Use Wrench interaction flow to trigger assembly checks.
3. Validation hooks run through `ra_lib_multiblock` and type-specific validators.
4. If valid, a multiblock marker is created and configured.
5. Tick dispatch processes the active multiblock each tick.
6. Structure failure or break event disassembles and cleans up.

Assembly and validity are intentionally separated:

- `ra_lib_multiblock` handles generic lifecycle.
- `ra_multiblock` handles type-specific logic and recipes.

## Implemented Structures

### Blast Forge

Location: `data/ra_multiblock/function/blast_forge`

Includes:

- directional validation and check dispatch
- process logic
- fuel consumption and heat behavior
- recipe matching (`smelting`, `recycling`)

Heat behavior summary:

- Processing requires minimum heat.
- Fuel items increase heat by tier.
- Passive decay reduces heat over time.
- Processing interval scales with heat range.

### Upgrade Platform

Location: `data/ra_multiblock/function/upgrade_platform`

Includes:

- structure validate/check
- tick dispatch
- upgrade recipe execution (`tier_upgrades`)

### Rock Metallic Drill

Location: `data/ra_multiblock/function/drills/rock_metallic`

The worked example of the registry API below: the whole structure is one spec in
`ra_multiblock:register_types`, plus a tick function and one dispatch line.

Copper tier. Side view, drill facing north:

```text
B M     B = barrel (input and output)   M = copper base
C S     C = iron bars (drill shaft)     S = smooth stone
```

Produces one Rock every 40 ticks while a redstone block is not on its control
side.

## Adding a Multiblock

Declare the structure **once**, facing north, relative to the base block. The
library rotates it into the other three facings — the four hand-maintained
coordinate tables the older structures carry are what made this error-prone.

```mcfunction
data modify storage ra:multiblock spec set value {
  id:"rock_metallic_drill",
  name:"Rock Metallic Drill",
  tier:"copper",
  blocks:[
    {x:0,y:0,z:0,  match:"minecraft:waxed_copper_block"},
    {x:0,y:0,z:-1, match:"minecraft:barrel"},
    {x:0,y:-1,z:-1,match:"minecraft:iron_bars"},
    {x:0,y:-1,z:0, match:"minecraft:smooth_stone"}
  ],
  inputs:[{name:"input_1",x:0,y:0,z:-1}],
  outputs:[{name:"output_1",x:0,y:0,z:-1}],
  controls:[{name:"redstone_in",x:0,y:0,z:1}]
}
function ra_lib_multiblock:register with storage ra:multiblock spec
```

Then write a tick function and add its dispatcher to `#ra_lib_multiblock:tick`.
Assembly, rotation, periodic validation, disassembly and the
`ra.multiblock.{id}` marker tag are all handled for you. Setting `tier` is enough
for the wrench to find it — no wrench file needs editing.

`match` is passed straight to `execute if block`, so block ids, `#block_tags` and
block states all work. Set `rotates:0b` for a rotationally symmetric structure.

### IO Helpers

Reach a named input or output without ever branching on facing:

| Function | Purpose |
|---|---|
| `io/at {name,run}` | Run a function positioned at that IO block |
| `io/insert {name,id,count,components}` | Insert an item there |
| `io/extract {name,id,count}` | Remove items; 1 only if the full amount was taken |
| `io/peek {name}` | Copy the first stack to `storage ra:multiblock io_item` |
| `io/count {name}` | Number of occupied slots |
| `io/is_block {name,block}` | Test the block at that position |

These resolve names against the marker's own IO maps, so they work for the
hand-written multiblocks too.

Full API: `redstone_additions/src/data/ra_lib_multiblock/README.md`.

## Data and Direction Sources

Key assets:

- `storage ra:multiblock` — the type registry and per-facing derived data

Marker data model contains:

- `data.type`
- `data.facing`
- `data.properties`
- `data.inputs`
- `data.outputs`
- `data.controls`

Type-specific runtime fields are commonly added in `data.data` and `data.status`.

## Hook Surfaces

A registered type only needs two entries:

- `#ra_lib_multiblock:load` — its registration function
- `#ra_lib_multiblock:tick` — its tick dispatcher

The library covers the rest generically:

- `#ra_lib_multiblock:validate` — `generic/validate`
- `#ra_lib_multiblock:check_structure` — `generic/check_structure`
- `#ra_lib_multiblock:setup_type` — `generic/setup_type`
- `#ra_lib_multiblock:on_break` — optional, only if teardown needs more than
  removing the marker

Write your own hook only for a structure whose shape cannot be expressed as a
list of required blocks. Blast Forge and Upgrade Platform predate the registry
and still use hand-written validators; they work, so leave them unless you are
deliberately migrating them.

## Debug Checklist

1. Confirm base marker exists at the expected position.
2. Confirm only one active multiblock marker occupies the assembly center.
3. Verify validation functions return success before marker setup.
4. Inspect marker `data.type`, `data.facing`, and property fields.
5. If immediate disassembly occurs, inspect periodic structure check path.
6. Verify wrench staging data exists in `storage ra:multiblock` before assembly call.

---
