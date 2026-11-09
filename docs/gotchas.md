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
`!redstone_additions/src/main/kotlin/**/lib/`, and `redstone_additions/.gitignore`
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

## Macro lines need a variable

A line starting with `$` must contain at least one `$(name)`. One that does not raises
"No variables in macro" at load and Minecraft rejects the **whole function**, not just the
line — how the Big Torch once shipped without its torch (the macro's only variable was replaced
by a constant and the leftover `$` took the function down). `PackValidator` fails generation on
such a line in strict and report mode alike, naming the function and line. Fix it by dropping
the `$` (the line no longer needs to be a macro) or by putting the variable back.

## Every macro variable must be in the compound

`function f with storage s path` fails as a whole when `f` names any `$(name)` that the compound
at `path` lacks — even on a line an `execute if` would have skipped. Nothing is logged and no
command in `f` runs. When one function serves compounds of different shapes (settings rows of
several types, say), dispatch to one macro per shape and let each name only fields its shape
always has. v6.0.0's `ra_settings:act/user` read `$(min)`/`$(max)`/`$(step)` for its int
branch, so every bool row's click failed and personal toggles never flipped.

## Calls through macros are unchecked

`PackValidator` checks every `function ns:path` and `function #ns:tag` in the generated
functions, every entry of the pack's function tags (unless marked `required: false`) and every
advancement reward function. It skips any path containing a macro
(`function ra_jetpacks:items/give_$(kit)_kit`), since only the game can resolve those. A typo
there is caught in game only. Keep macro-built paths to the cases that need them, and list their
possible values in a comment.

## Load messages are gated

Anything printed to chat while `#minecraft:load` runs must honour the server setting *Show the
load message* (`global.welcome`). Print it with `loadMessage(json)` (or `moduleLoaded(text,
color)` for a module's "loaded!" line), which prefixes
`execute unless data storage ra:settings global{welcome:0b} run`. The gate reads storage rather
than `ra_settings:get`, because module loads run before `ra_settings:init` creates its objectives.
A warning that must show whatever the setting (the disabled-blocks notice) goes through
`loadNotice(line)` instead. Lines for `@a[tag=ra.debug]` and `@s` are allowed as they are.

The build follows every call from `#minecraft:load` (through function tags, not `schedule`) and
fails on any other `tellraw`. v6.0.1 gated only the hub's greeting, so with the setting off a
reload still printed eleven module lines.

## Don't load two builds at once

An old beet release (v5.1.17 and earlier) and the Kore build write the same namespaces. With
both in a world, every function exists twice, the later pack wins file by file, and `ra:tick` is
scheduled by both. Remove the old pack (`redstone_additions_data_pack`, or a release zip named `redstone_additions_v5.x…`) before testing the Kore build.

## A module property named like a registry list resolves to the registry

Inside `fun Ra.register()` declared in a module `object`, an unqualified name is looked up on the
extension receiver (`Ra`) **before** the enclosing object. A module's own
`private val blocks = listOf(...)` was therefore silently replaced by the registry's list of
every block registered so far, and `ra_gates:tick` / `load` / `give_all` ran every earlier
module's blocks a second time. It compiled because both are lists of `CustomBlock`.

The registry list is now `Ra.allBlocks`. Still: give module-level collections specific names
(`gateBlocks`), or declare them as locals inside `register()`, where they shadow correctly.

## `set string` accepts a number

`data modify storage s p set string storage s v` succeeds when `v` is a number too: it writes the
number's digits as a string. It fails only for a list or a compound. Used as "is `v` a string?",
it called every number a string — which is how the Data Handler gave numeric properties a text
editor until 6.0.1. To tell types apart, use a test only that type passes:

- number: two `/compute`s of a storage provider with different fallbacks,
  `compute default integer {type:"minecraft:storage",storage:"s",path:"v",fallback:0}` and the
  same with `fallback:1`. The provider returns its fallback when the value is absent or not a
  number (a string, even `"5"`), so the results agree exactly when `v` is a number. Two
  fallbacks, not one "impossible" sentinel: every int is a possible value.
- list: `data modify storage s v insert 0 value 0b` succeeds only on a list (copy it first).
  `if data storage s v[]` is no test: an empty list has no element to match.
- compound: `execute if data storage s v{}`.
- byte flag: `execute if data storage s {v:1b}` — an NBT match is typed, so the int `1` fails it.

If you need "is it a string?" without those, `set string` and then `set from` over the copy: the
second changes nothing only when the value was a string already (`DataHandlerList.isText`).

## Kore's `compute { }` can hoist a provider out of a macro line

Kore's own `compute { }` command may move an inline context int provider into a registry file
and reference it by id. A provider whose storage path holds `$(id)` then loses its macro
substitution. Build `/compute` lines with `ra/core/Compute.kt` (`computeScore`, `computeInto`,
`computeStorage`), which render the provider as SNBT in the command itself and add the `$`
prefix when the line contains `$(`. See
[Arithmetic with `/compute`](developer-guide.md#arithmetic-with-compute).

## `/compute` cancels where a scoreboard operation wraps

A scoreboard `*=` that leaves 32 bits wraps round; the same product in `/compute` cancels the
whole evaluation, and so does a division by zero. Under `execute store result` the target gets
**0**, and `data modify … set compute` writes nothing — no error in the log either way. The
bridge fill ratio (`amount * 10000 / capacity`) read 0 on any network above ~214,748 mL for
this reason until it moved to a float provider (`computeScoreScaled`). Guard divisors, and
divide in floats when a scaled product can grow large.

Also: scoreboard `/=` and `%=` round towards negative infinity. Their `/compute` equivalents are
`floor_div` and `floor_mod` (Kore `floorDiv`, `floorMod`), not `div` and `mod`.

## A property pasted into a macro is part of the command

`$execute as $(entity_selector) run tag @s add $(tag)` does not receive a selector: it receives
text, and the game parses the whole line after substituting it. A player who can set
`entity_selector` to `@a[limit=1] if function <any> run say` makes the block run any function,
with the server's permission; `"}` in a value pasted inside quotes ends the string. Until 6.0.1
the sensors, the Message Block, the Boxer/Unboxer, the vaults and the Teleport Anchor all did
this with values any Data Handler could write.

Declare such a property with its [text format](tools-and-registries.md#text-formats)
(`text("entity_selector", TextFormat.SELECTOR)`) and call `guardProperty(name, format)`
(`ra/lib/safe/SafeText.kt`) on the marker before the macro call. Better still, do not paste it
at all: compare two strings with `data modify storage s a set from storage s b` (success 0 means
equal), as the Teleport Anchor and the Wireless Receiver do. When it must be pasted whatever it
holds (a Goggles line), run it through `ra_lib:safe/clean` first (`cleanFrom(source, format)`,
see [the core sanitiser](developer-guide.md#safe-player-input-in-commands)).

The reverse trap: `interpret:true` on an `nbt` text component does **not** parse a string. Since
1.21.5 it decodes the stored tag as a component directly, so the string `{text:'x',click_event:…}`
shows as exactly those characters. Only a stored compound is rich text. A player who can only
write strings cannot add a click event through it.
