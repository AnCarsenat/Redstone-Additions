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
	RedstoneAdditions.build(output).generate()
	shipLooseFiles(File(output, RedstoneAdditions.PACK_NAME))
}

/**
 * Files that ship inside the pack folder next to `pack.mcmeta` without being pack content:
 * the self-updater (`python updater.py` run from inside the pack updates it from Modrinth; it
 * finds its pack by sitting in it) and the changelog. The beet build copied them with
 * `keep_loose_files.py`; Kore only writes what it generates, so they are copied here. Paths are
 * relative to the project directory, which is the working directory under the Gradle plugin.
 */
private val LOOSE_FILES = listOf("updater.py", "CHANGELOG.md")

private fun shipLooseFiles(pack: File) {
	for (name in LOOSE_FILES) {
		val source = File(name)
		check(source.isFile) { "$name not found in ${File("").absolutePath}; it ships inside the pack" }
		source.copyTo(File(pack, name), overwrite = true)
	}
}
