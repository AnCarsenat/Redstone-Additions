# Porting History

Up to v5.1.17 Redstone Additions was hand-written `.mcfunction` / JSON, built by
[beet](https://github.com/mcbeet/beet) from `redstone_additions/src/data/`. It is now Kotlin that
[Kore](https://kore.ayfri.com) turns into the data pack, in `redstone_additions/`.

This page records why and how that move was made, what was kept and what changed, and how a
port was checked against the old build. For working on the pack today, start at
[Getting Started](getting-started.md).

!!! note "Layout during the port, and after"
    While the port ran, both trees were in the repository: the beet pack in
    `redstone_additions/` (sources in `src/data/`, committed build in
    `build/redstone_additions_data_pack/`) and the Kore project beside it in
    `redstone_additions_kore/`. Once every part was ported and reviewed, the beet tree was
    removed and `redstone_additions_kore/` was renamed to `redstone_additions/`. The
    `redstone_additions/src/data/…` and `build/redstone_additions_data_pack/…` paths below are
    therefore the **beet** layout, which now only exists in the git history and in the v5.1.17
    release. The tools worth keeping (`md_to_bbcode.py`, `recipe_render/`, `compare_reference.py`)
    moved to the repository-root `tools/`; `compare_reference.py` now takes the reference pack as
    `--reference <folder>`, an unzipped release rather than an in-tree build.

The rewrite changed how the pack is **written**, not its behaviour: the same commands, in the
same functions, at the same paths.

## Why Kore

The beet pack is about 1,400 files and 19,000 lines of mcfunction. Most of its bugs came from
two sources, both recorded in the beet-era developer guide:

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

The [Porting Guide](porting-history.md#differences-that-are-expected) lists the few structural
differences the rewrite allows.

## What changes

| | beet | Kore |
| --- | --- | --- |
| Source | `redstone_additions/src/**/*.mcfunction`, `*.json` | `redstone_additions/src/main/kotlin/ra/**/*.kt` |
| Build | `beet build` (Python 3.14, beet 0.116) | `./gradlew koreBuild` (JDK 25, Kore 2.28.0-26.3) |
| Minecraft versions | 1.21.9 – 26.3 (formats 88 – 121) through overlays | **26.3 only** (format 121) |
| 26.3 shapes | rewritten at build time by `tools/port_26_3.py` into `overlay_121/` | written directly by Kore |
| Macro checks | `tools/lint_macros.py` | typed `nbt { }` macro arguments + `PackValidator`, which also took over its "macro line with no `$(variable)`" check |
| Settings | `tools/settings/*.json` + `tools/settings_gen.py` | Kotlin data in `ra.settings.pages`, generator in `ra.settings.gen` |
| Placement handlers, goggles dispatch, wrench / read-only / Data Handler registries, give bundles | hand-edited per block | built from the `Ra` registry |
| Version | typed in six files (see the old release checklist) | `version` in `redstone_additions/gradle.properties` |

Supporting older Minecraft versions was dropped on purpose. Kore checks its output against one
game version only, and the overlays plus `port_26_3.py` existed only to bridge shapes that changed
inside the old range.

## Status

A part counts as ported when `tools/compare_reference.py <namespaces>` reports nothing beyond the
[expected differences](porting-history.md#differences-that-are-expected) and a reviewer has gone through
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
function.

Behaviour changes made after the port, on purpose (see the [changelog](changelog.md)):

- Teleport Anchors teleport again. `find_dest` had been empty since v5.1.8; it now tags the
  nearest other anchor whose `anchor_id` matches, with the id tested inside the selector.
- Enchant crafting: the first sacrifice recipe that matches wins and keeps its own chance.

## Porting guide

How a beet namespace became a Kore part, and how a port was proven to change nothing.

### Sources of truth

| What | Where | Use it for |
| --- | --- | --- |
| beet source | `redstone_additions/src/data/<namespace>/` | the logic **and the comments** |
| what 26.3 loaded | `redstone_additions/build/redstone_additions_data_pack/data/`, with `overlay_102/` then `overlay_121/` on top | the exact 26.3 shape of predicates, item modifiers, recipe unlocks, `block_display` states and inline loot SNBT |
| generated settings | `redstone_additions/build/.../data/ra_settings/` | `settings_gen.py` output, which has no source file |
| Kore docs | [kore.ayfri.com/docs](https://kore.ayfri.com/docs) | API usage |
| Kore source | [github.com/Ayfri/Kore](https://github.com/Ayfri/Kore), `kore/src/commonMain/kotlin/io/github/ayfri/kore/` | exact signatures |
| Kore generated types | the `kore-jvm-2.28.0-26.3.jar` in `~/.gradle/caches/` | `Items`, `Blocks`, `RecipeArgument`… (not in the source tree; see [Kore Gotchas](gotchas.md#generated-types-live-only-in-the-jar)) |

When the source and `overlay_121` disagree, `overlay_121` is what the game ran on 26.3. Port
that shape.

### Order of work

1. **Read the whole namespace first**, comments included. Many lines exist because of a bug
   the comment describes; a port that "simplifies" them reintroduces it.
2. **Data files**: recipes, advancements, predicates, item modifiers, tags. Use typed builders and
   generate families from data. Correctness here is mechanically checkable, so it is a good first
   slice.
3. **Functions, block by block**, one Kotlin file per block, with the
   [custom block helpers](custom-blocks.md).
4. **Registration**: `ra.block`, wrench rows, read-only properties, bundles, hooks
   ([Tools and Registries](tools-and-registries.md)), and the module's `load` / `tick`.
5. **Build and compare** until only expected differences remain:

   ```bash
   ./gradlew koreBuild -Pra.strict=false
   python3 tools/compare_reference.py <namespaces> --diff
   ```

6. **Review**, a second pass by someone else: every beet function against its Kotlin, command
   for command.

### Mapping

| beet | Kore |
| --- | --- |
| `data/ns/function/a/b.mcfunction` | `pack.define(ref(Ns.X, "a/b")) { … }` / `pack.fn(Ns.X, "a/b") { … }` |
| `function ns:a/b` | `function(ref)` |
| `function ns:a/b {k:"v"}` | `function(ref, nbt { this["k"] = "v" })` |
| `function ns:a/b with storage ra:temp x` | `function(ref, Storage.TEMP, "x")` |
| `function #ns:tag` | `function(ns, tag, group = true)` or a `functionTag(...)` argument |
| `$cmd $(arg)` | `raw("\$cmd \$(arg)")` |
| `schedule function ns:a 1t` | `schedule(ref).replace(1.ticks)` |
| `tags/function/t.json` entry | `pack.hook(Ns.X, "t", fn)` in the part owning `fn` |
| entry in `#ra:placement_handlers` | `ra.block(block)` |
| line in `ra:tools/goggles/draw_block` | `ra.block(block)` |
| line in `ra:tools/goggles/draw_multiblock` | `ra.multiblockGoggles += TagRoute(...)` |
| entry in `ra:tools/wrench/init_registry` | `ra.wrenchCycle(type, WrenchCycle(...))` |
| branch in `ra:tools/wrench/found_block` | `ra.wrenchToggles += TagRoute(...)` |
| entry in `ra:tools/readonly/init_registry` | `ra.readonly(type, props…)` |
| `ra:items/bundles/give_<x>_bundle` | `ra.bundles += GiveBundle(...)` |
| `tools/settings/<page>.json` | a `Page` in `ra/settings/pages/` |
| `recipe/<id>.json` + `advancement/recipes/unlock_<id>.json` | `blockRecipe` / `recipeUnlock` |
| `give.mcfunction`, the on-break drop, the bundle entry, the recipe result | derived from one `CustomBlock` |

### Reading the comparison

`compare_reference.py` prints four kinds of entries (see
[Getting Started](getting-started.md#what-it-ignores) for the normalisation it applies first):

| Entry | Means | Do |
| --- | --- | --- |
| `MISSING` | the beet build had the file, Kore does not write it | port it, unless it is on the list below |
| `EXTRA` | Kore writes a file beet did not | expected for the structural changes below; otherwise a stray path |
| `DIFFERS` | both have it, normalised contents differ | read the `--diff` |
| (identical) | | nothing |

### Differences that are expected

| Difference | Why |
| --- | --- |
| A block's `tick` differs and a new `on_break` exists | `blockTick` / `blockOnBreak` route each broken marker through one function ([Custom Blocks](custom-blocks.md#breaking-the-standard-shape-and-its-variants)) |
| `MISSING ra_enchanting/README.md`, `ra_lib_multiblock/README.md` | module READMEs are not shipped; their content moved to the docs |
| `schedule function ra:tick 1t` → `schedule function ra:tick 1 replace` | Kore spells the unit-less tick count and the default mode out; same behaviour |
| `tp` → `teleport` | alias |
| Registry-built files list entries in a different but equivalent order | only where order is not observable (the placement tag keeps registration order; bundles keep `HAND_OUT_ORDER`) |
| SNBT key order inside a compound (`{text:…,italic:0b,color:…}`) | compounds are unordered |
| `dx=2.0` for `dx=2` | the same number |

Anything else is a porting bug until shown otherwise: a missing command, a different number, a
different selector filter, a changed **order of commands** inside a function, a renamed macro
argument, a different storage path, an early `return` in a different place.

#### Beet bugs kept on purpose

A port reproduces behaviour, including behaviour that looks wrong. A bug found while porting
gets a comment beside the line and is fixed in its own later change, so the port's diff stays
reviewable. Found during the port reviews, still to be commented and fixed:

- `ra_settings:act/user` is called `with storage ra:settings cur`; the review flagged the macro
  arguments of its int branch as a beet bug carried over unchanged (`settings/act/ActFns.kt`).

### Things the beet build did that Kore no longer needs

| beet | Replaced by |
| --- | --- |
| `tools/port_26_3.py`, `overlay_121/` | Kore writes the 26.3 shapes directly |
| `overlay_102/` | the 26.3 shape is the only one |
| `tools/lint_macros.py` | typed `nbt { }` macro arguments + `PackValidator`, which also took over its "macro line with no `$(variable)`" check |
| `tools/keep_loose_files.py` | `Main.kt` copies the two loose files that ship, `updater.py` and `CHANGELOG.md`, into the pack folder; module READMEs live in the docs |
| `tools/settings_gen.py` | `ra.settings.gen` |
