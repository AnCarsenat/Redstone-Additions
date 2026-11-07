# Kore Gotchas

Things that cost time during the rewrite, with the symptom first so you can find them.

## Item components in JSON

**Symptom:** generation throws while writing a recipe:
`SerializationException: No tag in stack for requested element`, or
`NbtEncodingException: … Expected Encoder to be NbtEncoder, got … JsonTreeEncoder`.

**Cause:** in Kore 2.28, the typed `itemName(...)`, `customData { }` and `entityData { }`
components serialize only to SNBT. They work in `give` and `summon`, and throw inside a recipe
result, which is JSON.

**Fix:** set them as Kore's `CustomComponent`, which holds raw NBT and writes both formats:

```kotlin
this["item_name"] = CustomComponent(NbtString(name))
this["custom_data"] = CustomComponent(nbt { this["ra"] = nbt { this[id] = true } })
this["entity_data"] = CustomComponent(entityDataNbt())
```

`CustomBlock.blockItemComponents()` and `ItemSpec.applyTo()` already do. Keys go
**without** the `minecraft:` prefix, or the same scope writes some components prefixed and some
not.

## Generated types live only in the jar

**Symptom:** `Unresolved reference 'RecipeArgument'` (or `AdvancementArgument`,
`PredicateArgument`, `Items`, `Blocks`, `EntityTypes`, `Tags`…) and grepping the Kore repository
finds no declaration.

**Cause:** these are generated from the game data at Kore's build time and only exist in the
published jar.

**Fix:** import them from `io.github.ayfri.kore.generated.*`
(`io.github.ayfri.kore.generated.arguments.types.RecipeArgument`). To find a package:

```bash
unzip -l ~/.gradle/caches/modules-2/files-2.1/io.github.ayfri.kore/kore-jvm/2.28.0-26.3/*/kore-jvm-2.28.0-26.3.jar | grep RecipeArgument
```

## `lib/` and `build` in `.gitignore`

**Symptom:** a fresh clone does not compile: `Unresolved reference 'RaLibPart'`, or a
`multiblocklib/build/` class is missing. Locally everything works.

**Cause:** the repository `.gitignore` ignores `lib/` (Python virtualenvs), which also matches the
Kotlin package `ra/lib/`. The Kore project's own `.gitignore` used to ignore a bare `build`, which
also matched `ra/multiblocklib/build/`. The files existed on disk and were never committed.

**Fix (in place):** the root `.gitignore` un-ignores
`!redstone_additions_kore/src/main/kotlin/**/lib/`, and `redstone_additions_kore/.gitignore`
anchors its entries (`/build`, `/out`, `/.gradle`). If you add a package named after a common
build or cache folder, run `git status --ignored` before you commit.

## Selector arguments

- Kore writes selector arguments **sorted** (`@e[distance=..0.5,limit=1,sort=nearest,tag=…,type=…]`),
  whatever order you set them in. Equivalent in game; `compare_reference.py` sorts both sides.
- Setting a filter twice **appends**: `tag = A; tag = B` writes `tag=A,tag=B`. That is how to
  require two tags. It is also how to add a filter by accident.
- Kore prefixes ids (`type=minecraft:marker`), also equivalent.

## `execute { run { … } }` with several commands

Kore moves a multi-command `run { }` into its own function under `generated_scopes/`, named
after a hash of its body, in the **pack's default namespace** (`redstone_additions`), which is
not one of the pack's namespaces. Nothing in the pack does this today, and it should stay that
way: those functions are invisible to `PackValidator` and impossible to find in game. When a
`run` needs several commands, define a real function with `fn(...)` and call it.

## Commands Kore spells differently

| You write | Kore writes | Same in game? |
| --- | --- | --- |
| `schedule(tick).replace(1.ticks)` | `schedule function ra:tick 1 replace` | yes: a unit-less time is ticks, `replace` is the default |
| `tp(self(), vec3())` | `teleport @s ~ ~ ~` | yes |
| `fill(from, to, block, filter)` | `fill … <block> replace <filter>` | — this **is** the replace form; there is no `FillOption.REPLACE` |
| `vec3(-1, -1, -1).relative` | `~-1 ~-1 ~-1` | — |
| a `Double` in `nbt { }` | `0.45d` | yes |
| a `Boolean` in `nbt { }` | `1b` / `0b` | yes; SNBT `true` / `false` are bytes |

## SNBT booleans when parsing

knbt's SNBT parser does not accept `true` / `false`, which the game reads as `1b` / `0b`.
`ra.core.item.parseSnbt` rewrites them before parsing; do the same if you parse game-style SNBT.

## Gradle skips generation

**Symptom:** you expect the validator's warnings and see nothing.

**Cause:** `koreBuild` is up to date, so Gradle did not run it.

**Fix:** `./gradlew koreBuild --rerun`. Also note that generation reads only the runtime classpath;
a file the generator reads from disk must be declared in `kore { additionalInputs.from(...) }`
to trigger a rebuild.

## Calls through function tags and macros are partly unchecked

`PackValidator` checks `function #ns:tag` against the tags the pack declares, but not what is
**in** the tag. It skips any path containing a macro (`function ra_jetpacks:items/give_$(kit)_kit`),
since only the game can resolve those. A typo there is caught in game only. Keep macro-built
paths to the cases that need them, and list their possible values in a comment.

## Don't load two builds at once

The beet build and the Kore build write the same namespaces. With both in a world, every
function exists twice, the later pack wins file by file, and `ra:tick` is scheduled by both.
Remove `redstone_additions_data_pack` before testing the Kore build.

## A module property named like a registry list resolves to the registry

Inside `fun Ra.register()` declared in a module `object`, an unqualified name is looked up on the
extension receiver (`Ra`) **before** the enclosing object. A module's own
`private val blocks = listOf(...)` was therefore silently replaced by the registry's list of
every block registered so far, and `ra_gates:tick` / `load` / `give_all` ran every earlier
module's blocks a second time. It compiled because both are lists of `CustomBlock`.

The registry list is now `Ra.allBlocks`. Still: give module-level collections specific names
(`gateBlocks`), or declare them as locals inside `register()`, where they shadow correctly.
