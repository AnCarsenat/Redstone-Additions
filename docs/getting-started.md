# Getting Started

## Prerequisites

| Tool | Version | Why |
| --- | --- | --- |
| JDK | **25** | `build.gradle.kts` sets `jvmToolchain(25)`, and the Kore plugin's AOT cache needs 25+. |
| Python | 3.10+ | Only for `tools/compare_reference.py`. Not needed to build. |

Nothing else needs installing. The Gradle wrapper (`gradlew`, `gradle/wrapper/`) downloads
Gradle, and Gradle downloads Kotlin, Kore and the Kore Gradle plugin, at the versions pinned in
`gradle/libs.versions.toml`:

```toml
[versions]
kore = "2.28.0-26.3"   # Kore 2.28.0, targeting Minecraft 26.3
kotlin = "2.4.20"
```

## Building

```bash
cd redstone_additions_kore
./gradlew koreBuild
```

| Output | Where |
| --- | --- |
| The pack | `build/kore/redstone_additions/` (`pack.mcmeta`, `pack.png`, `data/`) |
| AOT cache | `build/kore-aot/` (speeds up later generations; safe to delete) |

`koreBuild` compiles the Kotlin, runs `ra.MainKt` with the output directory in the `kore.output`
system property, and wipes the output folder first (`cleanBeforeBuild`), so a function removed
from the Kotlin also disappears from the pack.

Gradle skips `koreBuild` when nothing it tracks changed. To force a fresh generation, for
example to see the validator's output again:

```bash
./gradlew koreBuild --rerun
```

### Strict mode

Every generation ends with `PackValidator` (`ra/core/PackValidator.kt`). It reads every line of
every generated function and checks that each `function ns:path` and `function #ns:path` into
one of the pack's namespaces points at a function or function tag the pack defines. Calls whose
path contains a macro (`give_$(kit)_kit`) are skipped, since only the game can resolve them.

| Mode | Command | On an unresolved call |
| --- | --- | --- |
| strict (default) | `./gradlew koreBuild` | generation fails, listing every caller and missing target |
| report | `./gradlew koreBuild -Pra.strict=false` | prints `[ra] N unresolved call(s), strict mode off:` and the list, then writes the pack |

`-Pra.strict` reaches the generator as the `ra.strict` system property (see `build.gradle.kts`).
Report mode is for work in progress: a half-written part can still be generated and compared.
A release is always built strict.

## Putting it in a world

```bash
./gradlew koreRun -Pworld="Redstone Test"        # a world under <minecraft dir>/saves
./gradlew koreRun -Pworld="/path/to/saves/My World"
./gradlew koreRun -Pworld                        # ra.world from local.properties
./gradlew koreRun -Pworld --continuous           # rebuild and reinstall on every save
```

Then `/reload` in game. Without `-Pworld`, `koreRun` builds but installs nowhere. The full
reference (the untracked `local.properties`, launcher instances, RCON, removing the pack again)
is [Installing into a world](contributing-guidelines.md#installing-into-a-world).

!!! warning "Never load the beet build and the Kore build together"
    Both write the same namespaces. With both enabled every function exists twice, the pack
    that loads last wins file by file, and `ra:tick` is scheduled by both: you get a mix of two
    versions running at once. Remove `redstone_additions_data_pack` from the world first.

## Comparing against the beet build

`tools/compare_reference.py` diffs the Kore output against the committed beet build. The
reference is what 26.3 actually loaded:
`redstone_additions/build/redstone_additions_data_pack/data/`, with `overlay_102/` and then
`overlay_121/` laid over it file by file.

```bash
./gradlew koreBuild -Pra.strict=false
python3 tools/compare_reference.py ra_gates            # summary for one namespace
python3 tools/compare_reference.py ra_gates ra_sensors # several
python3 tools/compare_reference.py ra_gates --diff     # plus a unified diff per mismatch
python3 tools/compare_reference.py --all
python3 tools/compare_reference.py ra_gates --strict   # no normalisation (see below)
```

Output:

```
reference files: 56  kore files: 61
identical: 49  different: 7  missing: 0  extra: 5
  EXTRA    ra_gates/function/blocks/clock/on_break.mcfunction
  DIFFERS  ra_gates/function/blocks/clock/tick.mcfunction
```

It exits 0 only when nothing is missing, extra or different.

### What it ignores

The comparison removes differences that never change what the game does. `--strict` turns all
of them off except the first two.

| Normalisation | Example |
| --- | --- |
| Comment and blank lines are dropped | `# Tick all clocks` |
| Runs of whitespace collapse to one space | |
| `minecraft:` is removed everywhere (commands, JSON values, JSON keys) | `minecraft:iron_block` = `iron_block` |
| Selector arguments are sorted | `@e[type=marker,tag=x]` = `@e[tag=x,type=marker]` |
| Quotes around a bare SNBT word are removed | `{block_tag:"clock"}` = `{block_tag:clock}` |
| The `d` suffix on a double is removed | `y:0.45d` = `y:0.45` |
| SNBT booleans become bytes | `italic:false` = `italic:0b` |
| A one-element list of ids equals the bare id (JSON) | `"items": ["x"]` = `"items": "x"` |
| `"replace": false` on tags and `"frame": "task"` on advancements are dropped (JSON) | the game's own defaults |
| A call to a `generated_scopes/` function is expanded back into its lines, each prefixed with the calling `execute … run` | Kore moving a multi-command `run { }` into its own function |

What is left after that is a real difference. The [Porting Guide](porting-history.md#reading-the-comparison)
explains how to read it.

## IDE

Open `redstone_additions_kore/` in IntelliJ IDEA and let it import the Gradle project. Running
`ra.MainKt` directly works: without the plugin's `kore.output` property, `Main.kt` writes to
`out/` in the working directory, and `RedstoneAdditions.VERSION` reads `version` from
`gradle.properties` in that same directory (see [Release and Versioning](release.md)).
