# Porting Guide

How a beet namespace becomes a Kore part. The beet source is
`redstone_additions/src/data/<namespace>/`; what the game actually loaded on 26.3 is
`redstone_additions/build/redstone_additions_data_pack/` with `overlay_102/` and `overlay_121/`
applied — read the build when a source file and the 26.3 shape disagree (predicates, item
modifiers, recipe unlocks, `block_display` states, inline loot SNBT).

## Order of work

1. Read every file of the namespace first, including its comments: they explain decisions the
   commands do not.
2. Port data-driven files (recipes, advancements, predicates, item modifiers, tags) with typed
   builders, generating families from data.
3. Port functions block by block, one Kotlin file per block, using the
   [custom block helpers](custom-blocks.md).
4. Register what other parts need (registry, hooks) and fill in the module's `load` / `tick`.
5. `./gradlew koreBuild -Pra.strict=false`, then `python3 tools/compare_reference.py <ns> --diff`.
6. Fix every difference that is not on the list below.

## Mapping

| beet | Kore |
|---|---|
| `data/ns/function/a/b.mcfunction` | `pack.define(ref(Ns.X, "a/b")) { … }` or `pack.fn(Ns.X, "a/b") { … }` |
| `function ns:a/b` | `function(ref)` |
| `function ns:a/b {k:"v"}` | `function(ref, nbt { this["k"] = "v" })` |
| `function ns:a/b with storage ra:temp x` | `function(ref, Storage.TEMP, "x")` |
| `$cmd $(arg)` | `raw("\$cmd \$(arg)")` |
| `tags/function/t.json` entry | `pack.hook(Ns.X, "t", fn)` |
| `#ra:placement_handlers` entry | `ra.block(block)` |
| a line in `ra:tools/goggles/draw_block` | `ra.block(block)` (or `ra.goggles += TagRoute(...)`) |
| a line in a `give_all` / bundle | the module's `GiveBundle` |
| `ra:tools/wrench/init_registry` entry | `ra.wrenchCycle(...)` |
| `ra:tools/readonly/init_registry` entry | `ra.readonly(...)` |
| `ra:tools/data_handler/init_registry` entry | `ra.dataHandlerProperties(...)` |
| `tools/settings/<page>.json` | a settings page in `ra.settings` |
| recipe + `advancement/recipes/unlock_*.json` | `blockRecipe` / `recipeUnlock` |

## Reading the comparison

`compare_reference.py` reports four kinds of entries:

- **MISSING** — a file the beet build had and Kore does not write. Port it, or (rarely) record
  why it is gone in your part's notes.
- **EXTRA** — a file Kore writes that beet did not. Expected for the structural changes below.
- **DIFFERS** — run with `--diff`. After normalisation, what is left is real.
- identical — nothing to do.

### Differences that are expected

- **Per-block `tick` and a new `on_break`.** `blockTick` / `blockOnBreak` replace the
  `tag ra.broken` + one-line-per-step pattern with one `on_break` per broken marker.
- **Dispatch functions built from the registry** (`draw_block`, placement handler tag, bundles,
  wrench/dh/readonly registries) may list entries in a different but equivalent order.
- **`generated_scopes/` functions** — the comparison expands them back inline.
- **Item component spelling in commands** (`item_name=Clock` vs `item_name="Clock"`), already
  normalised away.
- **Advancement / tag JSON defaults** Kore always writes (`"replace": false`).

Anything else — a missing command, a different number, a different selector filter, a changed
order of commands inside a function — is a porting bug until shown otherwise.

## Things the beet build did that Kore no longer needs

- `tools/port_26_3.py` and `overlay_121/` — Kore writes the 26.3 shapes directly.
- `overlay_102/` — superseded by the 26.3 shape.
- `tools/lint_macros.py` — macro calls go through typed `nbt { }` arguments, and
  `PackValidator` checks every call resolves.
- `tools/keep_loose_files.py` — the pack ships no loose files; `CHANGELOG.md` and module
  READMEs move to the docs.
- `tools/settings_gen.py` — the settings pages are Kotlin data, and the generated functions are
  written from them directly.
