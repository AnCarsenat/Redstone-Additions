# Multiblocks (Kore)

How to add a multiblock structure from Kotlin. The gameplay side is in [Multiblocks](../multiblocks.md);
the library's runtime (assembly, rotation, validation, disassembly) is unchanged from the beet
pack and its contract is in `redstone_additions/src/data/ra_lib_multiblock/README.md`.

Two parts are involved:

| Part | Namespace | Package | Role |
| --- | --- | --- | --- |
| `MultiblockLibPart` | `ra_lib_multiblock` | `ra.multiblocklib` | the library: registry, assembly, rotation, validation, IO |
| `MultiblockModule` | `ra_multiblock` | `ra.modules.multiblock` | the Multiblock Base tiers and the concrete structures |

## The library's public surface

`ra.api.MultiblockLib`:

| Reference | Path | Use |
| --- | --- | --- |
| `register` | `ra_lib_multiblock:register` | register a structure from `storage ra:multiblock spec` |
| `tryAssemble`, `tryTier` | `try_assemble`, `try_tier` | called by the wrench on a base |
| `validateAll` | `validate_all` | the periodic integrity pass |
| `disassemble` | `disassemble` | teardown |
| `Io.at`, `Io.isBlock` | `io/at`, `io/is_block` | reach a named IO position of the marker's structure |
| `init` | `init` | called from `ra:load` |
| `Hooks.LOAD`, `TICK`, `VALIDATE`, `SETUP_TYPE`, `CHECK_STRUCTURE`, `ON_BREAK` | `#ra_lib_multiblock:<name>` | the hook tags |

The six hook tags are declared empty by the library (`Registry.kt`, `declareHookTags`) so its own
`function #ra_lib_multiblock:*` calls resolve whatever has hooked in. Structures add themselves
with `pack.hook`.

## Adding a structure: the registry path

New structures describe themselves as data; the library does the rest. The Rock Metallic Drill
(`modules/multiblock/drills/RockMetallicDrill.kt`) is the worked example.

**1. The spec**, written once for a structure facing **north**, offsets relative to the base:

```kotlin
const val SPEC = """{id:"rock_metallic_drill",name:"Rock Metallic Drill",tier:"copper",
	hint:"Need: barrel in front of the base, ...",
	blocks:[{x:0,y:0,z:0,match:"minecraft:waxed_copper_block"},{x:0,y:0,z:-1,match:"minecraft:barrel"},...],
	inputs:[{name:"input_1",x:0,y:0,z:-1}],
	outputs:[{name:"output_1",x:0,y:0,z:-1}],
	controls:[{name:"redstone_in",x:0,y:0,z:1}]}"""
```

| Field | Meaning |
| --- | --- |
| `id` | the type; the marker gets `ra.multiblock.<id>` (`Tag.multiblock(id)`) |
| `name` | shown by the goggles |
| `tier` | the base tier the wrench assembles it from |
| `hint` | what the wrench tells a player whose structure is incomplete |
| `blocks` | required blocks; `match` is a block id, block state or `#tag`, passed to `execute if block` |
| `inputs`, `outputs`, `controls` | named IO positions |
| `rotates` | optional; `0b` for a rotationally symmetric structure |

The library's `build/*` functions derive the south, east and west variants from it. There is no
per-facing coordinate table to keep in step.

**2. Register it** from `#ra_lib_multiblock:load`. `MultiblockModule` owns one function for all
its structures:

```kotlin
pack.define(registerTypes) {
	raw("data modify storage ra:multiblock spec set value ${RockMetallicDrill.SPEC}")
	function(MultiblockLib.register, Storage.MULTIBLOCK, "spec")
}
pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.LOAD, registerTypes)
```

The registry is rebuilt from scratch on every load, so a removed or edited type cannot linger.

**3. Tick it.** A dispatcher selects the type's markers and runs its logic, hooked into
`#ra_lib_multiblock:tick`:

```kotlin
pack.define(tickDispatch) {
	execute {
		asTarget(allEntities { type = EntityTypes.MARKER; tag = Tag.multiblock(ID) })
		at(self())
		run { function(tick) }
	}
}
pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
```

**4. Use named IO**, never facing-specific coordinates. The marker carries IO maps the library
already rotated to its facing:

```kotlin
function(MultiblockLib.Io.isBlock, nbt { this["name"] = "redstone_in"; this["block"] = "minecraft:redstone_block" })
```

That is all a registry-described structure needs. Validation, the setup that writes `type`,
`facing`, `properties`, `inputs`, `outputs`, `controls` onto the marker, and the integrity check
come from the library's `generic/*` functions.

### Why `generic/*` is registered last

`generic/check_structure`, `generic/setup_type` and `generic/validate` cover every type nobody
else handled, so they must run **after** every structure's own hook in the same tag. The beet
tags listed them last. Since the multiblock library registers before the modules, it queues
those three hooks with `ra.afterAllParts { … }`
([Architecture](architecture.md#after-all-parts-afterallparts)).

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

They work; leave them alone unless you are deliberately migrating one to a spec. Their hook
order inside each tag is Blast Forge, Upgrade Platform, Drill, which is the order
`MultiblockModule.register` calls them in.

## Bases

The Multiblock Base is **one** block type with five tiers (copper, iron, gold, diamond,
netherite). `BaseTier` (`modules/multiblock/blocks/BaseTier.kt`) describes each tier once and
produces its item, give and placement data; `Bases.kt` registers them, marks `tier` and
`tier_level` read-only, and handles breaking. The wrench's plain-RMB on a base tries to assemble
(`tools/wrench/WrenchMultiblock.kt`).

## Goggles

Register a readout per multiblock type with `multiblockGoggles += TagRoute(markerTag, fn)`; Tools
builds `ra:tools/goggles/draw_multiblock` from it. Readouts use the multiblock billboard writers
in `GogglesRefs.Billboard` (`handle_multiblock`, `show_input`, `show_output`, `show_control`) and
`Goggles.Status.readStatusMultiblock`.

## Ticking order

`ra_multiblock:tick`:

1. Break detection for every base tier (`Bases.checkBreak`).
2. Every 40 ticks, `ra_lib_multiblock:validate_all`.
3. `function #ra_lib_multiblock:tick`: every structure's dispatcher, in hook order.
