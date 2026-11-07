package ra.multiblocklib

import ra.core.PackPart
import ra.core.Ra
import ra.multiblocklib.Assembly.registerAssembly
import ra.multiblocklib.Validation.registerValidation
import ra.multiblocklib.build.Build.registerBuild
import ra.multiblocklib.generic.Generic.registerGeneric
import ra.multiblocklib.io.Io.registerIo
import ra.multiblocklib.structure.Structure.registerStructure

/**
 * Multiblock library (`ra_lib_multiblock`): assembly, validation and IO for multiblock structures.
 *
 * Register a structure once, facing north, with `ra_lib_multiblock:register` from a function in
 * `#ra_lib_multiblock:load`, and add its tick dispatcher to `#ra_lib_multiblock:tick`. Assembly,
 * per-facing rotation, periodic validation, disassembly and the `ra.multiblock.{id}` tag are all
 * handled here. The hook tags are listed in [ra.api.MultiblockLib.Hooks]; only `load` and `tick`
 * need an entry for a registered type, the generic validator / tagger / checker cover the rest.
 */
object MultiblockLibPart : PackPart {
	override fun Ra.register() {
		declareHookTags()
		registerLifecycle()
		registerAssembly()
		registerValidation()
		registerBuild()
		registerStructure()
		registerGeneric()
		registerIo()
	}
}
