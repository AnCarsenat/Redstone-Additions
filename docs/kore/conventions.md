# Conventions

## Structure

- **Kotlin path mirrors generated path.** `ns:blocks/<id>/*` is written by one file,
  `ra/modules/<module>/<Block>.kt`; a library folder `ra_lib:redstone/*` by files under
  `ra/lib/redstone/`.
- **Files stay under ~300 lines.** Split along generated paths, never by "misc".
- **`DataPack` extensions register, `Function` extensions emit.** `fun Ra.registerClock()`
  declares functions and resources; `private fun Function.process()` writes commands into the
  function being built.
- **Inline by default, extract a real function when something must reference it** (a tag,
  `schedule`, a macro call, another file, or the beet version had it as a function players or
  other packs may call). Inlined Kotlin helpers cost nothing at runtime.

## Names and identifiers

- Namespaces come from `Ns`. Shared objectives, tags and storages from `Obj`, `Tag`, `Storage`
  in `core/Ids.kt`; module-private ones as `private const val` in the module's own file.
- Every identifier written into a world (tag, objective, storage path, `data.*` key, item
  component) is a compatibility contract. Keep the beet spelling exactly.
- Functions are values (`ref`, `block.fn`, `api` objects). **Never** write a call to one of the
  pack's own functions as a string inside `raw()`; use `function(ref)` /
  `function(ref, nbt { … })`. Kotlin catches the typo; `PackValidator` catches the missing
  definition.

## Commands: typed DSL first

Use the Kore builders — `execute { }`, `scoreboard.players.*`, `tag(...)`, `data(...) { }`,
`function(...)`, `summon`, `kill`, `fill`, `give`, `tellraw` with `textComponent` — wherever they
express the command at least as clearly as vanilla.

`raw("…")` is allowed for:

- macro lines (`$execute … $(x) …`);
- long SNBT literals copied from the game (display transformations, item stacks);
- commands Kore has no builder for, or selectors/conditions the builder makes markedly longer
  than the vanilla text.

Each `raw` is exactly one complete 26.3 command (it rejects newlines). If a raw command repeats
across files, it is a helper waiting to be written — add it to the part's package, or to
`core/` if several parts need it.

## Comments

The beet source is heavily commented with **why** a line is the way it is ("`store result`
writes zero when the read fails, and zero means every tick"). Those comments are the most
valuable thing in it. Carry them over as Kotlin comments next to the code they explain; drop
comments that only restate the command. Do not emit them into the `.mcfunction` output.

## Data-driven files

Use Kore's typed builders (`recipesBuilder`, `advancement`, `predicate`, `itemModifier`,
`lootTable`, tags). Generate families from data — one `listOf(...)` of tiers, one loop — instead
of one Kotlin block per near-identical file (`ra_lib:light/at_least_1..15`, jetpack
`fit_iron_0..7`, the recipe unlock advancements).

## Kore gotchas

- Generated registries and argument types (`Items`, `Blocks`, `EntityTypes`, `Tags`,
  `RecipeArgument`, `AdvancementArgument`…) live in `io.github.ayfri.kore.generated.*` and are
  only in the Kore jar, not in its source tree.
- Typed `itemName` / `customData` / `entityData` components break JSON output; use
  `CustomComponent(nbt)` for items that appear in data files. See
  [Custom Blocks](custom-blocks.md#item-components-gotcha).
- `execute { run { … } }` with more than one command writes a `generated_scopes/` function.
  That is fine; give it a real name with `fn(...)` when it is worth finding in game.
- Selector filters set twice append (`tag = A; tag = B` → `tag=A,tag=B`).
- `fill(from, to, block, filter)` is the replace form; `vec3(x, y, z).relative` gives `~x ~y ~z`.
