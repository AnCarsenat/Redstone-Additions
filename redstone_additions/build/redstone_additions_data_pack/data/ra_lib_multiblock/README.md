# ra_lib_multiblock

Assembly, validation and IO for multiblock structures.

## Adding a multiblock

Register the structure once, facing **north**, and the library derives the other
three facings itself.

```mcfunction
# ra_multiblock:register_types   (listed in #ra_lib_multiblock:load)

data modify storage ra:multiblock spec set value {
  id:"rock_metallic_drill",
  name:"Rock Metallic Drill",
  tier:"copper",
  hint:"Need: barrel in front of the base, iron bars under it, smooth stone under the base",
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

Then write a tick function and add its dispatcher to `#ra_lib_multiblock:tick`:

```mcfunction
execute as @e[type=marker,tag=ra.multiblock.rock_metallic_drill] at @s run function <your>:tick
```

That is the whole job. Assembly, per-facing rotation, periodic structure
validation, disassembly and the `ra.multiblock.{id}` tag are all handled.

### Fields

| Field | Meaning |
|---|---|
| `id` | Type id. Becomes the marker tag `ra.multiblock.{id}`. |
| `name` | Human-readable name. |
| `tier` | Base block tier the wrench assembles it from (`copper`…`netherite`). |
| `hint` | Optional text for failure messages. |
| `rotates` | `0b` for a rotationally symmetric structure; all facings then use the authored offsets unchanged. Defaults to rotating. |
| `blocks` | Required blocks as `{x,y,z,match}`, relative to the base. `match` is passed straight to `execute if block`, so ids, `#block_tags` and block states all work. |
| `inputs` / `outputs` / `controls` | Named positions as `{name,x,y,z}`. |

### Coordinates

Offsets are relative to the base block, for a structure facing **north**, where
"in front of the base" is `-Z`. `ra_lib_multiblock:build` produces south, east
and west by quarter turns about the vertical axis.

This is the part that used to be manual. The Blast Forge still carries four
hand-written coordinate tables of 26 positions each — 104 offsets rotated by
hand, every one of them a chance to transpose a sign.

## IO helpers

All of these take the marker as `@s`, positioned at the base, and resolve names
against the marker's own `data.inputs` / `data.outputs` / `data.controls`. They
work for any multiblock that went through `setup_marker`, including the
hand-written ones — a tick function never needs to branch on facing.

| Function | Purpose |
|---|---|
| `io/at {name,run}` | Run a function positioned at the named IO block. |
| `io/insert {name,id,count,components}` | Insert an item into the named container. |
| `io/extract {name,id,count}` | Remove items; returns 1 only if the full amount was taken. |
| `io/peek {name}` | Copy the first stack to `storage ra:multiblock io_item`; returns 1 if present. |
| `io/count {name}` | Number of occupied slots. |
| `io/is_block {name,block}` | Test the block at that position; returns 1 on a match. |

For items with components, build `storage ra:multiblock io_item` yourself and
call `io/at {name:"...",run:"ra_lib_multiblock:io/insert_here"}` — that avoids
quoting a component compound through a macro argument.

## Hooks

| Tag | When |
|---|---|
| `#ra_lib_multiblock:load` | Register types. |
| `#ra_lib_multiblock:tick` | Per-type tick dispatch. |
| `#ra_lib_multiblock:validate` | Assembly. Registered types are covered by `generic/validate`. |
| `#ra_lib_multiblock:check_structure` | Periodic integrity check. Registered types are covered by `generic/check_structure`. |
| `#ra_lib_multiblock:setup_type` | Marker tagging. Registered types are covered by `generic/setup_type`. |
| `#ra_lib_multiblock:on_break` | Optional cleanup on disassembly. |

Only `load` and `tick` need an entry for a registered type.
