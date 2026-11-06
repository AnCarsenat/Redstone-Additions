# Getting Started

## Prerequisites

- **JDK 25.** The Gradle toolchain asks for 25 and the Kore plugin uses its AOT cache.
- Nothing else: the Gradle wrapper downloads Gradle, Kotlin and Kore.

## Building

```bash
cd redstone_additions_kore
./gradlew koreBuild
```

The pack is written to `build/kore/redstone_additions/`. Generation also runs
`PackValidator`, which fails the build on a call to a function the pack never defines.

While only part of the pack is ported, other parts are missing and their calls cannot resolve.
Turn the failure into a printed list with:

```bash
./gradlew koreBuild -Pra.strict=false
```

## Playing it

List the worlds the plugin can see, then name the ones to install into in
`build.gradle.kts`:

```bash
./gradlew koreWorlds
```

```kotlin
kore {
	worlds = listOf("Redstone Test")
}
```

```bash
./gradlew koreRun                # build + copy into every listed world
./gradlew koreRun --continuous   # rebuild and recopy on every save
```

Then `/reload` in game. A dedicated server with RCON enabled reloads by itself; see the Kore
[Gradle plugin](https://kore.ayfri.com/docs/guides/gradle-plugin) page for the `rcon { }` block.

!!! warning "Remove the beet pack first"
    Both packs use the same namespaces. Loading the beet build and the Kore build in one world
    runs every function twice.

## Comparing against the beet build

`tools/compare_reference.py` diffs the Kore output against the committed beet build
(`redstone_additions/build/redstone_additions_data_pack/`, with its 26.3 overlays applied):

```bash
./gradlew koreBuild -Pra.strict=false
python3 tools/compare_reference.py ra_gates          # summary for one namespace
python3 tools/compare_reference.py ra_gates --diff   # with a diff per mismatch
python3 tools/compare_reference.py --all
```

It ignores differences that never change behaviour: comments and blank lines, selector
argument order, `minecraft:` prefixes, optional SNBT quotes, the `d` suffix on doubles, and
Kore's `generated_scopes/` indirection (a call to a generated scope is expanded back into the
lines it holds). `--strict` turns all of that off.

See the [Porting Guide](porting.md#reading-the-comparison) for how to read the result.

## IDE

Open `redstone_additions_kore/` in IntelliJ IDEA. The `ra.MainKt` run configuration generates
into `out/` (the plugin's `kore.output` property is absent there, so `Main.kt` falls back to it).
