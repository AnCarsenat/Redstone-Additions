package ra

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.chatcomponents.textComponent
import io.github.ayfri.kore.arguments.colors.Color
import io.github.ayfri.kore.configuration
import io.github.ayfri.kore.dataPack
import io.github.ayfri.kore.iconPath
import io.github.ayfri.kore.path
import io.github.ayfri.kore.pack.pack
import ra.advancements.AdvancementsPart
import ra.core.GameplayModule
import ra.core.PackPart
import ra.core.PackValidator
import ra.core.Ra
import ra.example.ExamplePart
import ra.hub.HubPart
import ra.lib.RaLibPart
import ra.migrations.MigrationsPart
import ra.modules.chunkloader.ChunkLoaderModule
import ra.modules.enchanting.EnchantingModule
import ra.modules.ender.EnderModule
import ra.modules.gates.GatesModule
import ra.modules.infinite.InfiniteModule
import ra.modules.interactive.InteractiveModule
import ra.modules.jetpacks.JetpacksModule
import ra.modules.multiblock.MultiblockModule
import ra.modules.sensors.SensorsModule
import ra.modules.storage.StorageModule
import ra.modules.wireless.WirelessModule
import ra.modules.wires.WiresModule
import ra.multiblocklib.MultiblockLibPart
import ra.settings.SettingsPart
import ra.tools.ToolsPart

/** The whole pack: which parts it is made of, in which order, and its metadata. */
object RedstoneAdditions {
	/**
	 * The pack version, e.g. `5.1.17`. Single source of truth: `version` in
	 * `redstone_additions_kore/gradle.properties`, passed in by the Kore Gradle plugin as the
	 * `ra.version` system property. A run without the plugin (the IDE's `MainKt`) reads the same
	 * file from the working directory, so the version is never written down twice.
	 */
	val VERSION: String = System.getProperty("ra.version")
		?: java.io.File("gradle.properties").takeIf { it.isFile }?.readLines()
			?.firstNotNullOfOrNull { Regex("""^\s*version\s*=\s*(\S+)""").find(it)?.groupValues?.get(1) }
		?: "dev"
	const val PACK_NAME = "redstone_additions"

	/**
	 * Gameplay modules. `ra:load` and `ra:tick` call them in this order, which is the order the
	 * beet version hard-coded in `ra:load` / `ra:tick`.
	 */
	val modules: List<GameplayModule> = listOf(
		InteractiveModule,
		StorageModule,
		SensorsModule,
		GatesModule,
		WirelessModule,
		WiresModule,
		ChunkLoaderModule,
		MultiblockModule,
		EnchantingModule,
		InfiniteModule,
		JetpacksModule,
		EnderModule,
	)

	/**
	 * Every part, in registration order. Parts that build shared dispatch functions from the
	 * registry ([ToolsPart]: goggles and wrench; [HubPart]: load, tick, bundles) come last so
	 * that everything has registered by the time they read it.
	 */
	val parts: List<PackPart> = listOf(RaLibPart, MultiblockLibPart, SettingsPart, MigrationsPart) +
		modules +
		listOf(ExamplePart, AdvancementsPart, ToolsPart, HubPart)

	/**
	 * Strict mode fails generation on a call to an undefined function. Turned off with
	 * `-Pra.strict=false` while only part of the pack is ported; the problems are then printed.
	 */
	private val strict get() = System.getProperty("ra.strict", "true").toBoolean()

	fun build(output: String): DataPack = dataPack(PACK_NAME) {
		path(output)
		iconPath("pack.png")

		configuration {
			prettyPrint = true
			prettyPrintIndent = "  "
		}

		pack {
			// min/max format default to the version Kore targets (26.3); the pack supports
			// exactly that version, so they are not set here.
			description = textComponent("⚡ ", Color.RED) { bold = true } +
				textComponent("Redstone Additions", Color.GOLD) { bold = true } +
				textComponent(" v$VERSION\n", Color.YELLOW) +
				textComponent("Automate ", Color.GRAY) { italic = true } +
				textComponent("Everything", Color.WHITE) { italic = true; bold = true } +
				textComponent(" • ", Color.DARK_GRAY) +
				textComponent("by ", Color.GRAY) +
				textComponent("Ancar", Color.AQUA) { underlined = true }
		}

		val ra = Ra(this)
		for (part in parts) with(part) { ra.register() }
		ra.runDeferred()

		if (strict) PackValidator.validate(this) else PackValidator.report(this)
	}
}
