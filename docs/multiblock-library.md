# Multiblock Library

How to add a multiblock structure from Kotlin. The gameplay side is in [Multiblocks](multiblocks.md);
the library's runtime contract — coordinates, IO helpers, hooks — is at the
[end of this page](#runtime-contract).

Two parts are involved:

| Part | Namespace | Package | Role |
| --- | --- | --- | --- |
| `MultiblockLibPart` | `ra_lib_multiblock` | `ra.multiblocklib` | the library: registry, assembly, rotation, validation, IO |
| `MultiblockModule` | `ra_multiblock` | `ra.modules.multiblock` | the Multiblock Base tiers and the concrete structures |

## The library's public surface

`ra.api.MultiblockLib`:

| Reference | Path | Use |
| --- | --- | --- |
| `register` | `ra_lib_multiblock:register` | register a structure from `storage ra:multiblock spec` (add-ons) |
| `tryAssemble`, `tryTier` | `try_assemble`, `try_tier` | called by the wrench on a base |
| `validateAll` | `validate_all` | the periodic integrity pass |
| `disassemble` | `disassemble` | teardown |
| `Io.at`, `Io.isBlock` | `io/at`, `io/is_block` | reach a named IO position of the marker's structure |
| `init` | `init` | called from `ra:load` |
| `Hooks.LOAD`, `TICK`, `VALIDATE`, `SETUP_TYPE`, `CHECK_STRUCTURE`, `ON_BREAK` | `#ra_lib_multiblock:<name>` | the hook tags |

Kotlin structures use `MultiblockSpec`, `multiblockSpec { }` and `Ra.multiblock(spec)` from
`ra.api` instead of `register` ([below](#adding-a-structure-a-kotlin-spec)).

The six hook tags are declared empty by the library (`Registry.kt`, `declareHookTags`) so its own
`function #ra_lib_multiblock:*` calls resolve whatever has hooked in. Structures add themselves
with `pack.hook`.

## Adding a structure: a Kotlin spec

Structures written in Kotlin describe themselves with a `MultiblockSpec` (`ra/api/MultiblockSpec.kt`)
and are **compiled when the pack is generated**. The Rock Metallic Drill
(`modules/multiblock/drills/RockMetallicDrill.kt`) is the worked example.

**1. The spec**, written once for a structure facing **north**, offsets relative to the base:

```kotlin
val SPEC = multiblockSpec("rock_metallic_drill", "Rock Metallic Drill", "copper") {
	hint = "Need: barrel in front of the base, iron bars under the barrel, smooth stone under the base"
	block(0, 0, 0, "minecraft:waxed_copper_block")
	block(0, 0, -1, "minecraft:barrel")
	block(0, -1, -1, "minecraft:iron_bars")
	block(0, -1, 0, "minecraft:smooth_stone")
	input("input_1", 0, 0, -1)
	output("output_1", 0, 0, -1)
	control("redstone_in", 0, 0, 1)
}
```

| Builder | Meaning |
| --- | --- |
| `multiblockSpec(id, name, tier) { … }` | `id` is the type (`[a-z0-9_]`); the marker gets `ra.multiblock.<id>` (`spec.tag`). `name` is shown by the goggles, `tier` is the base tier the wrench assembles it from |
| `hint` | what the wrench tells a player whose structure is incomplete |
| `block(x, y, z, match)` | one required block. `match` is a block id, a `#tag`, or an id with states (`minecraft:furnace[lit=true]`); NBT is not supported |
| `blocks(match, pos(x, y, z), …)` | the same block at several offsets |
| `box(xs, ys, zs, match) { except }` | every position of an inclusive box, minus those `except` rejects — a 3x3 floor is `box(-1..1, -1..-1, -2..0, BRICKS)` |
| `input`, `output`, `control(name, x, y, z)` | named IO positions |
| `rotates = false` | rotationally symmetric: one predicate, and the marker's facing is ignored when checking |
| `symmetricFacing` | the `data.facing` a non-rotating structure is assembled with (`"north"` by default, or `"none"`) |

**2. Register it** from the part's `register()`:

```kotlin
fun Ra.registerRockMetallicDrill() {
	multiblock(SPEC)
	pack.define(tickDispatch) { forEachMarker(SPEC.tag, tick) }
	pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
}
```

`Ra.multiblock` only records the spec; the library compiles every recorded spec once all parts
have registered (registering from inside an `afterAllParts` block is an error).

**3. Tick it**, as above: a dispatcher hooked into `#ra_lib_multiblock:tick` selects the type's
markers and runs its logic. Hook `#ra_lib_multiblock:on_break` too if the type needs cleanup.

**4. Use named IO**, never facing-specific coordinates. The marker carries IO maps already
rotated to its facing:

```kotlin
function(MultiblockLib.Io.isBlock, nbt { this["name"] = "redstone_in"; this["block"] = "minecraft:redstone_block" })
```

### What the library generates

For every spec, from `multiblocklib/spec/Compiled.kt`:

| Output | Role |
| --- | --- |
| `ra_lib_multiblock:structure/<id>/<facing>` predicate (`structure/<id>` when `rotates = false`) | `all_of` of one `location_check` per block (`offsetX`/`offsetY`/`offsetZ` + block predicate). The offsets are rotated in Kotlin, so the whole pattern is one `execute if predicate` |
| `spec/<id>/validate`, `spec/<id>/accept/<facing>` | tries the facings' predicates; the winner sets `#mb_result` and writes `facing`, `inputs`, `outputs`, `controls` to storage as SNBT literals |
| `spec/<id>/check` | the periodic integrity check: the predicate of the marker's `data.facing`, stored into `@s ra.multiblock` |
| `spec/validate`, `spec/check_structure`, `spec/setup_type` | static dispatchers (one line per type, no macro, no registry walk), hooked after the hand-written structures and ahead of `generic/*` |
| `spec/try_tier`, `spec/tier/<tier>` | the compiled types of each tier, tried by `try_tier` before the storage registry |
| `spec/load` | called from `init` before `#ra_lib_multiblock:load`: writes `types.<id>` as one literal and appends the id to `registry` |

The predicates also make a per-block debug or a custom check easy to build: `spec.facings`,
`spec.blocksFacing(facing)`, `spec.portsFacing(spec.inputs, facing)` and `spec.predicate(facing)`
give the rotated data and the predicate id in Kotlin.

### The storage view stays

Everything that read `storage ra:multiblock` keeps working: `types.<id>` holds the authored spec
(`id`, `name`, `tier`, `hint`, `blocks`, `inputs`, `outputs`, `controls`, `rotates` when false)
plus `facings.<f>.{blocks,inputs,outputs,controls}` in exactly the shape `build/*` derives, and
`compiled:1b`. `registry` lists every type, compiled or not. The wrench, goggles, `io/*` and
add-ons see no difference.

## Adding a structure from an add-on: `ra_lib_multiblock:register`

A data pack that cannot run Kotlin registers its structure as data, at runtime. This path is
unchanged; the library rotates and checks these specs with its macro functions.

```mcfunction
# from a function listed in #ra_lib_multiblock:load
data modify storage ra:multiblock spec set value {id:"my_drill",name:"My Drill",tier:"iron",hint:"...",blocks:[{x:0,y:0,z:0,match:"minecraft:iron_block"},{x:0,y:0,z:-1,match:"minecraft:barrel"}],inputs:[{name:"input_1",x:0,y:0,z:-1}],outputs:[],controls:[]}
function ra_lib_multiblock:register with storage ra:multiblock spec
```

| Field | Meaning |
| --- | --- |
| `id` | the type; the marker gets `ra.multiblock.<id>` |
| `name` | shown by the goggles |
| `tier` | the base tier the wrench assembles it from |
| `hint` | what the wrench tells a player whose structure is incomplete |
| `blocks` | required blocks; `match` is a block id, block state or `#tag`, passed to `execute if block` (NBT works here) |
| `inputs`, `outputs`, `controls` | named IO positions |
| `rotates` | optional; `0b` for a rotationally symmetric structure |

`register` appends the id to `registry` and to `registry_runtime`. The library's `build/*`
functions derive the south, east and west variants of every `registry_runtime` type on load;
`generic/validate`, `generic/setup_type` and `generic/check_structure` validate, tag and check
them; `try_tier` walks `registry_runtime` after the compiled types. The registry is rebuilt from
scratch on every load, so a removed or edited type cannot linger.

### Why `generic/*` is registered last

`generic/check_structure`, `generic/setup_type` and `generic/validate` cover every type nobody
else handled, so they must run **after** every structure's own hook in the same tag. Since the
multiblock library registers before the modules, it queues those three hooks with
`ra.afterAllParts { … }` ([Architecture](architecture.md#after-all-parts-afterallparts)). The
compiled-spec dispatchers are queued the same way, just before them.

## The older structures

The Blast Forge and the Upgrade Platform predate the registry. They keep hand-written
per-facing data and their own hooks in every tag:

```kotlin
// modules/multiblock/blastforge/BlastForge.kt
pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.VALIDATE, validate)
pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.SETUP_TYPE, setupType)
pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.CHECK_STRUCTURE, checkStructure)
pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.ON_BREAK, onBreak)
multiblockGoggles += TagRoute(TAG, BlastForge.goggles)
```

They work; leave them alone unless you are deliberately migrating one to a spec. Their hooks
come first inside each tag, in the order `MultiblockModule.register` calls them in, followed by
the library's `spec/*` dispatchers and then `generic/*`.

## Bases

The Multiblock Base is **one** block type with five tiers (copper, iron, gold, diamond,
netherite). `BaseTier` (`modules/multiblock/blocks/BaseTier.kt`) describes each tier once and
produces its item, give and placement data; `Bases.kt` registers them, marks `tier` and
`tier_level` read-only, and handles breaking. The wrench's plain-RMB on a base tries to assemble
(`tools/wrench/WrenchMultiblock.kt`).

## Goggles

Register a readout per multiblock type with `multiblockGoggles += TagRoute(markerTag, fn)`; Tools
builds `ra:tools/goggles/draw_multiblock` from it. Readouts use the multiblock billboard writers
in `Goggles.Billboard` (`handle_multiblock`, `show_input`, `show_output`, `show_control`) and
`Goggles.Status.readStatusMultiblock`.

## Ticking order

`ra_multiblock:tick`:

1. Break detection for every base tier (`Bases.checkBreak`).
2. Every 40 ticks, `ra_lib_multiblock:validate_all`.
3. `function #ra_lib_multiblock:tick`: every structure's dispatcher, in hook order.

## Runtime contract

What the generated `ra_lib_multiblock` functions promise, for anyone calling them from Kotlin or from an addon pack.

### Coordinates

Offsets are relative to the base block, for a structure facing **north**, where
"in front of the base" is `-Z`. South, east and west are quarter turns about the
vertical axis — north `( x, y, z)`, south `(-x, y, -z)`, east `(-z, y, x)`, west
`( z, y, -x)` — computed in Kotlin for a compiled spec and by `build/*` for a
storage-registered one.

### IO helpers

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

### Hooks

| Tag | When |
|---|---|
| `#ra_lib_multiblock:load` | Register storage-described types. Compiled specs need no entry. |
| `#ra_lib_multiblock:tick` | Per-type tick dispatch. |
| `#ra_lib_multiblock:validate` | Assembly. Compiled types are covered by `spec/validate`, registered ones by `generic/validate`. |
| `#ra_lib_multiblock:check_structure` | Periodic integrity check. Covered by `spec/check_structure` / `generic/check_structure`. |
| `#ra_lib_multiblock:setup_type` | Marker tagging. Covered by `spec/setup_type` / `generic/setup_type`. |
| `#ra_lib_multiblock:on_break` | Optional cleanup on disassembly. |

A compiled type needs only `tick`; a storage-registered one needs `load` and `tick`.
