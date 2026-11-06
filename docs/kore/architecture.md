# Architecture

Kore is a generator: the Kotlin runs once on your machine and writes ordinary `.mcfunction`
and JSON files. A Kotlin `val` is never a scoreboard, and a Kotlin `if` decides what is
*written*, not what the game evaluates. Everything below is about organising the code that
writes the pack; the runtime model is the one described in [How It Works](../how-it-works.md).

## Project layout

```
redstone_additions_kore/
├── build.gradle.kts             Kore plugin config (packName, mainClass, worlds)
├── pack.png
├── tools/compare_reference.py   diff against the beet build
└── src/main/kotlin/ra/
    ├── Main.kt                  entry point: build + generate
    ├── RedstoneAdditions.kt     pack metadata, the list of parts, module order
    ├── core/                    the foundation every part uses
    │   ├── Ns.kt                every namespace
    │   ├── Ids.kt               shared objectives (Obj), entity tags (Tag), storages (Storage)
    │   ├── Fn.kt                ref / define / fn / raw / hook
    │   ├── Ra.kt                PackPart, GameplayModule, the Ra registry
    │   ├── PackValidator.kt     undefined-call check
    │   └── block/               CustomBlock and the helpers built on it
    ├── api/                     typed references to cross-namespace entry points
    │   ├── LibApi.kt            RaLib (ra_lib), MultiblockLib (ra_lib_multiblock)
    │   └── ToolsApi.kt          Goggles, Settings, ModulesApi
    ├── lib/                     ra_lib
    ├── multiblocklib/           ra_lib_multiblock
    ├── settings/                ra_settings
    ├── migrations/              ra_migrations
    ├── advancements/            ra_advancements
    ├── example/                 ra_example
    ├── tools/                   ra:tools/* (wrench, goggles, data handlers, clipboard, multimeter)
    ├── hub/                     ra:load, ra:tick, placement router, give bundles, uninstall
    └── modules/<module>/        one package per gameplay namespace
```

**Kotlin packages mirror generated paths.** `ra_gates:blocks/clock/process` is written by
`ra/modules/gates/Clock.kt`; `ra_lib:redstone/detect` by a file under `ra/lib/redstone/`. A
function seen in game maps back to a file without searching.

**No file over ~300 lines.** A block is one file. A library sub-system (`redstone`,
`transport`, `input`…) is a folder of files, one per concern. When a file grows past the limit,
split it along the generated paths it writes.

## Parts

The pack is a list of **parts**. Each implements `PackPart` and registers everything it owns
in `register()`:

```kotlin
interface PackPart {
	fun Ra.register()
}

interface GameplayModule : PackPart {
	val ns: Ns
	val title: String
	val load: FunctionArgument get() = ref(ns, "load")
	val tick: FunctionArgument? get() = ref(ns, "tick")
}
```

`RedstoneAdditions.parts` fixes the order:

| Order | Part | Namespace | Package |
|---|---|---|---|
| 1 | `RaLibPart` | `ra_lib` | `ra.lib` |
| 2 | `MultiblockLibPart` | `ra_lib_multiblock` | `ra.multiblocklib` |
| 3 | `SettingsPart` | `ra_settings` | `ra.settings` |
| 4 | `MigrationsPart` | `ra_migrations` | `ra.migrations` |
| 5 | the gameplay modules, in `RedstoneAdditions.modules` order | `ra_interactive` … `ra_ender` | `ra.modules.*` |
| 6 | `ExamplePart` | `ra_example` | `ra.example` |
| 7 | `AdvancementsPart` | `ra_advancements` | `ra.advancements` |
| 8 | `ToolsPart` | `ra` (`tools/*`) | `ra.tools` |
| 9 | `HubPart` | `ra` (load, tick, placement, items, uninstall) | `ra.hub` |

`RedstoneAdditions.modules` is also the order `ra:load` and `ra:tick` call module `load` /
`tick` functions in, which is the order the beet `ra:load` / `ra:tick` hard-coded.

## The registry

Parts never edit each other's files. When a part must tell the rest of the pack about
something, it registers it on `Ra`, and the part that owns the shared function builds it from
the registry. `ToolsPart` and `HubPart` run last for exactly that reason.

| Registry | Registered by | Built into |
|---|---|---|
| `blocks` | `ra.block(...)` | `ra:give_all_items` bundles (via the block's module), docs |
| function tag `#ra:placement_handlers` | `ra.block(...)` (or `pack.hook`) | the placement router |
| `goggles` | `ra.block(...)` | `ra:tools/goggles/draw_block`, `ra:tools/block_name` |
| `multiblockGoggles` | multiblock types | `ra:tools/goggles/draw_multiblock` |
| `wrenchCycles` | `ra.wrenchCycle(type, rows…)` | `storage ra:wrench cyclables` |
| `wrenchToggles` | `ra.wrenchToggles += …` | wrench plain-RMB dispatch |
| `readonly` | `ra.readonly(type, props…)` | `storage ra:dh readonly` |
| `dataHandlerProperties` | `ra.dataHandlerProperties(…)` | `storage ra:dh registry` |
| `bundles` | each module's `GiveBundle` | `ra:items/bundles/*` |

Function tags (`#ra_lib_multiblock:validate`, `#ra_enchanting:recipes`…) do not need a
registry: `pack.hook(ns, tag, fn)` appends to the tag file, so the part that owns a hook
function registers it itself.

## Function references

A function is named once, as a value, and that value is used both to define it and to call
it:

```kotlin
// api/LibApi.kt -- the contract
object RaLib { object Redstone { val detect = ref(Ns.LIB, "redstone/detect") } }

// lib/redstone/Detect.kt -- the definition
pack.define(RaLib.Redstone.detect) { /* ... */ }

// modules/gates/UniGate.kt -- a caller
function(RaLib.Redstone.detect)
```

Entry points other namespaces call live in `ra/api/`. Functions only their own part calls are
declared next to their definition (`val process = block.fn("process")`). Either way, never
write a call as a string: `PackValidator` checks string calls too, but a typo in a value is a
compile error, not a build-time one.

## Lifecycle

```
minecraft:load ─► ra:load ─► ra_lib:init, module loads (in order), ra_lib_multiblock:init,
                             settings, registries, ra_migrations:run, schedule ra:tick
ra:tick (rescheduled every tick) ─► tool tag cleanup, ra_settings:tick, ra_lib:transport/tick,
                             ra_lib:input/tick, data handler, placement router, module ticks,
                             goggles
```

`ra:tick` reschedules itself rather than sitting in `#minecraft:tick`, as in the beet version.
The order inside `ra:tick` is load-bearing (input sessions must be scanned before the clicks
that open them are handled); [How It Works](../how-it-works.md#2-tick-orchestration) explains why.
