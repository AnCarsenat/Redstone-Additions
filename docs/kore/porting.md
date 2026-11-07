# Porting Guide

How a beet namespace becomes a Kore part, and how to prove the port changed nothing.

## Sources of truth

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

## Order of work

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

## Mapping

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

## Reading the comparison

`compare_reference.py` prints four kinds of entries (see
[Getting Started](getting-started.md#what-it-ignores) for the normalisation it applies first):

| Entry | Means | Do |
| --- | --- | --- |
| `MISSING` | the beet build had the file, Kore does not write it | port it, unless it is on the list below |
| `EXTRA` | Kore writes a file beet did not | expected for the structural changes below; otherwise a stray path |
| `DIFFERS` | both have it, normalised contents differ | read the `--diff` |
| (identical) | | nothing |

## Differences that are expected

| Difference | Why |
| --- | --- |
| A block's `tick` differs and a new `on_break` exists | `blockTick` / `blockOnBreak` route each broken marker through one function ([Custom Blocks](custom-blocks.md#breaking-the-standard-shape-and-its-variants)) |
| `MISSING ra_enchanting/README.md`, `ra_lib_multiblock/README.md` | loose files are not shipped; their content moves to the docs |
| `schedule function ra:tick 1t` → `schedule function ra:tick 1 replace` | Kore spells the unit-less tick count and the default mode out; same behaviour |
| `tp` → `teleport` | alias |
| Registry-built files list entries in a different but equivalent order | only where order is not observable (the placement tag keeps registration order; bundles keep `HAND_OUT_ORDER`) |
| SNBT key order inside a compound (`{text:…,italic:0b,color:…}`) | compounds are unordered |
| `dx=2.0` for `dx=2` | the same number |

Anything else is a porting bug until shown otherwise: a missing command, a different number, a
different selector filter, a changed **order of commands** inside a function, a renamed macro
argument, a different storage path, an early `return` in a different place.

### Beet bugs kept on purpose

A port reproduces behaviour, including behaviour that looks wrong. A bug found while porting
gets a comment beside the line and is fixed in its own later change, so the port's diff stays
reviewable. Found during the port reviews, still to be commented and fixed:

- `ra_settings:act/user` is called `with storage ra:settings cur`; the review flagged the macro
  arguments of its int branch as a beet bug carried over unchanged (`settings/act/ActFns.kt`).

## Things the beet build did that Kore no longer needs

| beet | Replaced by |
| --- | --- |
| `tools/port_26_3.py`, `overlay_121/` | Kore writes the 26.3 shapes directly |
| `overlay_102/` | the 26.3 shape is the only one |
| `tools/lint_macros.py` | typed `nbt { }` macro arguments + `PackValidator` |
| `tools/keep_loose_files.py` | no loose files ship; changelog and READMEs live in the docs |
| `tools/settings_gen.py` | `ra.settings.gen` |
