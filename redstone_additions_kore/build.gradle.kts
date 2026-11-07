plugins {
	alias(libs.plugins.kotlin)
	alias(libs.plugins.kore)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.kore)
}

kotlin {
	jvmToolchain(25)
}

kore {
	// Must match the folder name written by `dataPack(...)` in RedstoneAdditions.kt.
	packName = "redstone_additions"
	mainClass = "ra.MainKt"

	// Worlds under `.minecraft/saves` the pack is copied into by `gradlew koreRun`.
	// `gradlew koreWorlds` lists what the plugin can see.
	worlds = listOf()

	systemProperties = mapOf(
		// `-Pra.strict=false` lets a partly ported tree generate; see RedstoneAdditions.strict.
		"ra.strict" to (findProperty("ra.strict")?.toString() ?: "true"),
		// The pack version, from `version` in gradle.properties; read by RedstoneAdditions.VERSION.
		"ra.version" to project.version.toString(),
	)
}
