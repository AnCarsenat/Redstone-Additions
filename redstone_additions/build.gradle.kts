import java.util.Properties

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
	// Loose files Main.kt copies into the pack; listed so a change to them reruns koreBuild.
	additionalInputs.from(file("updater.py"), file("CHANGELOG.md"))

	// Must match the folder name written by `dataPack(...)` in RedstoneAdditions.kt.
	packName = "redstone_additions"
	mainClass = "ra.MainKt"

	// Where `koreRun` / `koreLink` copy the pack: nowhere, unless `-Pworld` is given.
	// See "Installing into a world" in docs/contributing-guidelines.md.
	val target = worldTarget()
	when {
		target == null -> worlds = listOf()
		// A path: the world folder itself, or its datapacks folder.
		'/' in target || '\\' in target -> dataPackTargets = listOf(
			file(target).let { if (it.name == "datapacks") it else it.resolve("datapacks") }.absolutePath,
		)
		// A bare name: a world under <minecraft dir>/saves.
		else -> worlds = listOf(target)
	}
	localProperties().getProperty("ra.minecraftDir")?.let { minecraftDirectory = file(it) }

	systemProperties = mapOf(
		// `-Pra.strict=false` lets a partly ported tree generate; see RedstoneAdditions.strict.
		"ra.strict" to (findProperty("ra.strict")?.toString() ?: "true"),
		// The pack version, from `version` in gradle.properties; read by RedstoneAdditions.VERSION.
		"ra.version" to project.version.toString(),
	)
}

/** Untracked, per-contributor settings (`ra.world`, `ra.minecraftDir`). Missing file = empty. */
fun localProperties() = Properties().apply {
	val f = rootProject.file("local.properties")
	if (f.isFile) f.inputStream().use { load(it) }
}

/**
 * The world `-Pworld` points at.
 * - `-Pworld="My World"` or `-Pworld=/path/to/saves/My World`: that world.
 * - bare `-Pworld`: `ra.world` from local.properties.
 * - no `-Pworld`: null, nothing is linked.
 */
fun worldTarget(): String? {
	val flag = findProperty("world")?.toString() ?: return null
	return flag.ifBlank {
		localProperties().getProperty("ra.world")?.takeIf { it.isNotBlank() }
			?: throw GradleException("-Pworld was given without a value and local.properties has no ra.world. Use -Pworld=\"<world name or path>\" or set ra.world in redstone_additions/local.properties.")
	}
}
