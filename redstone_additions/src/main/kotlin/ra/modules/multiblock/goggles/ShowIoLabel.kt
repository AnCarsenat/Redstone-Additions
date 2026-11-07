package ra.modules.multiblock.goggles

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.core.Ns
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * `ra_multiblock:goggles/show_io_label`: a coloured I/O label above one multiblock IO block.
 * Shared by the multiblocks' goggles functions, so it lives here rather than in either one's
 * folder. As the multiblock marker, at the base.
 *
 * Macro: `$(x) $(y) $(z)` block offset from the base, `$(label)` text, `$(color)` text colour.
 * Labels are placed with the configurable multiblock offsets (storage ra:display).
 */
object ShowIoLabel {
	val fn = ref(Ns.MULTIBLOCK, "goggles/show_io_label")

	fun register(pack: DataPack) = pack.define(fn) {
		execute {
			unlessCondition { data(Storage.DISPLAY, "offsets.multiblock_io") }
			run { function(Goggles.Billboard.initOffsets) }
		}

		raw("\$data modify storage ra:temp io set value {x:$(x),y:$(y),z:$(z),label:\"$(label)\",color:\"$(color)\"}")
		for (axis in listOf("x", "y", "z")) data(Storage.TEMP) { modify("io.o$axis") { set(Storage.DISPLAY, "offsets.multiblock_io.$axis") } }
		data(Storage.TEMP) { modify("io.scale") { set(Storage.DISPLAY, "offsets.multiblock_io.scale") } }

		function(Goggles.Billboard.showIoLine)
	}
}
