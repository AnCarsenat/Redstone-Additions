# Kore Rewrite

Redstone Additions is being rewritten from hand-written `.mcfunction` / JSON files built by
[beet](https://github.com/mcbeet/beet) to Kotlin that [Kore](https://kore.ayfri.com) turns into a
data pack. This section is the reference for that rewrite: how the new project is laid out, how
to build and release it, and the contracts every part of it relies on.

The gameplay pages (modules, tools, settings) still describe what the pack **does**. The rewrite
changes how the pack is **written**, not its behaviour: the target is the same commands, in the
same functions, at the same paths.

## Pages

| Page | Read it when |
| --- | --- |
| [Getting Started](getting-started.md) | You want to build the pack, put it in a world, or check it against the beet build. |
| [Architecture](architecture.md) | You need the model: parts, the `Ra` registry, load and tick order. |
| [Project Reference](project-reference.md) | You need to know which Kotlin file writes a given `ns:path`. |
| [Custom Blocks](custom-blocks.md) | You are adding or changing a placeable block. |
| [Tools and Registries](tools-and-registries.md) | Your block needs the goggles, the wrench or the Data Handlers. |
| [Settings](settings.md) | You are adding or changing a setting. |
| [Multiblocks](multiblocks.md) | You are adding a multiblock structure. |
| [Conventions](conventions.md) | You are writing any Kotlin in the project. |
| [Porting Guide](porting.md) | You are moving beet source over, or reading `compare_reference.py` output. |
| [Release and Versioning](release.md) | You are bumping the version or cutting a release. |
| [Kore Gotchas](gotchas.md) | Something generates, compiles or loads wrongly and you do not know why. |

## Why Kore

The beet pack is about 1,400 files and 19,000 lines of mcfunction. Most of its bugs came from
two sources, both recorded in the old [Developer Guide](../developer-guide.md):

- **One fact typed in many places.** A block's identity was retyped in its give command, recipe
  result, on-break drop, bundle entry, placement tag, marker tag, goggles dispatch line,
  `give_all` list and unlock advancement. "ID mismatch between recipe result and placement
  handler tag" was the first entry under *Common Failure Modes*.
- **Calls nothing checks.** `function ra_lib:redstone/detct` loads, does nothing, and shows up
  only as one line in the game log after `/reload`.

In Kotlin both become build-time errors:

- A block is one [`CustomBlock`](custom-blocks.md) value. Every place that needs its id, name or
  item derives it from that value, so they cannot disagree.
- A function is one `FunctionArgument` value, used both to define it and to call it. A typo is a
  compile error, and `PackValidator` fails generation on a call to a function the pack never
  defines.
- Shared lists (placement handlers, goggles dispatch, wrench registry, give bundles) are
  [built from a registry](architecture.md#the-registry) that each block adds itself to. They are
  not edited by hand.

## What stays the same

The **runtime contract** is frozen, so existing worlds keep working and no player-facing
command changes:

- every namespace (`ra`, `ra_lib`, `ra_gates`…) and every function path;
- every entity tag, scoreboard objective, data storage and `data.*` key on markers;
- every item's components (`custom_data={ra:{<id>:1b}}`, the bat spawn egg shape);
- the order modules load and tick in.

The [Porting Guide](porting.md#differences-that-are-expected) lists the few structural
differences the rewrite allows.

## What changes

| | beet | Kore |
| --- | --- | --- |
| Source | `redstone_additions/src/**/*.mcfunction`, `*.json` | `redstone_additions_kore/src/main/kotlin/ra/**/*.kt` |
| Build | `beet build` (Python 3.14, beet 0.116) | `./gradlew koreBuild` (JDK 25, Kore 2.28.0-26.3) |
| Minecraft versions | 1.21.9 – 26.3 (formats 88 – 121) through overlays | **26.3 only** (format 121) |
| 26.3 shapes | rewritten at build time by `tools/port_26_3.py` into `overlay_121/` | written directly by Kore |
| Macro checks | `tools/lint_macros.py` | typed `nbt { }` macro arguments + `PackValidator` |
| Settings | `tools/settings/*.json` + `tools/settings_gen.py` | Kotlin data in `ra.settings.pages`, generator in `ra.settings.gen` |
| Placement handlers, goggles dispatch, wrench / read-only / Data Handler registries, give bundles | hand-edited per block | built from the `Ra` registry |
| Version | typed in six files (see the old release checklist) | `version` in `redstone_additions_kore/gradle.properties` |

Supporting older Minecraft versions was dropped on purpose. Kore checks its output against one
game version only, and the overlays plus `port_26_3.py` existed only to bridge shapes that changed
inside the old range.

## Status

A part counts as ported when `tools/compare_reference.py <namespaces>` reports nothing beyond the
[expected differences](porting.md#differences-that-are-expected) and a reviewer has gone through
it function by function. "Played in game" is a separate step and is not tracked here.

| Part | Namespace(s) | Package | Status |
| --- | --- | --- | --- |
| Foundation | — | `ra.core`, `ra.api` | Done |
| Tools | `ra` (`tools/*`, tool recipes / predicates / advancements) | `ra.tools` | Ported and reviewed |
| Hub | `ra` (load, tick, placement, items, uninstall), `minecraft` | `ra.hub` | Ported and reviewed |
| Migrations | `ra_migrations` | `ra.migrations` | Ported and reviewed |
| Advancements | `ra_advancements` | `ra.advancements` | Ported and reviewed |
| Settings | `ra_settings` | `ra.settings` | Ported and reviewed |
| Multiblock library | `ra_lib_multiblock` | `ra.multiblocklib` | Ported and reviewed |
| Logic Gates | `ra_gates` | `ra.modules.gates` | Ported and reviewed |
| Multiblocks | `ra_multiblock` | `ra.modules.multiblock` | Ported and reviewed |
| Example addon | `ra_example` | `ra.example` | Ported and reviewed |
| Transport Networks | `ra_wires` | `ra.modules.wires` | Ported and reviewed |
| Jetpacks | `ra_jetpacks` | `ra.modules.jetpacks` | Ported and reviewed |
| Enchant Crafting | `ra_enchanting` | `ra.modules.enchanting` | Ported and reviewed |
| Chunk Loader | `ra_chunk_loader` | `ra.modules.chunkloader` | Ported and reviewed |
| Infinite Generators | `ra_infinite` | `ra.modules.infinite` | Ported and reviewed |
| Shared library | `ra_lib` | `ra.lib` | Ported and reviewed |
| Interactive Machines | `ra_interactive` | `ra.modules.interactive` | Ported and reviewed |
| Storage | `ra_storage` | `ra.modules.storage` | Ported and reviewed |
| Ender Links | `ra_ender` | `ra.modules.ender` | Ported and reviewed |
| Wireless Redstone | `ra_wireless` | `ra.modules.wireless` | Ported and reviewed |
| Sensors | `ra_sensors` | `ra.modules.sensors` | Ported and reviewed |

Every part is ported, and the pack generates in strict mode: no call resolves to a missing
function. None of it has been played in game yet.
