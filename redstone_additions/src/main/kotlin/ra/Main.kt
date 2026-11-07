package ra

import java.io.File

/**
 * Entry point run by the Kore Gradle plugin (`gradlew koreBuild` / `koreRun`).
 *
 * The plugin passes its output directory as the `kore.output` system property; `out` is the
 * fallback when `main` is run straight from the IDE.
 */
fun main() {
	val output = System.getProperty("kore.output") ?: "out"
	// Checked before generating, so a missing file never leaves a half-written pack behind.
	val loose = LOOSE_FILES.map { File(it) }.onEach {
		check(it.isFile) { "${it.name} not found in ${File("").absolutePath}; it ships inside the pack" }
	}
	RedstoneAdditions.build(output).generate()
	val pack = File(output, RedstoneAdditions.PACK_NAME)
	loose.forEach { it.copyTo(File(pack, it.name), overwrite = true) }
}

/**
 * Files that ship inside the pack folder next to `pack.mcmeta` without being pack content:
 * the self-updater (`python updater.py` run from inside the pack updates it from Modrinth; it
 * finds its pack by sitting in it) and the changelog. The beet build copied them with
 * `keep_loose_files.py`; Kore only writes what it generates, so they are copied here. Paths are
 * relative to the project directory, which is the working directory under the Gradle plugin.
 */
private val LOOSE_FILES = listOf("updater.py", "CHANGELOG.md")
