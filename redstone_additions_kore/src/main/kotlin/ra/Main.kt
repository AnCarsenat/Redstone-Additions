package ra

/**
 * Entry point run by the Kore Gradle plugin (`gradlew koreBuild` / `koreRun`).
 *
 * The plugin passes its output directory as the `kore.output` system property; `out` is the
 * fallback when `main` is run straight from the IDE.
 */
fun main() {
	val output = System.getProperty("kore.output") ?: "out"
	RedstoneAdditions.build(output).generate()
}
