package ra.modules.storage

import io.github.ayfri.kore.functions.Function
import ra.core.block.CustomBlock
import ra.core.block.finishPlacement
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.setOnNew

/**
 * The `input1` / `output1` offsets the Boxer and Unboxer share: seeded at placement, defaulted
 * and migrated at tick time. Both are droppers or barrels that read one container and write
 * another, through the same ra_interactive item-mover entry points.
 */

/** As the spawned bat: place [block], seed `input1` = [input] and `output1` = front. Shared by the Boxer and Unboxer. */
internal fun Function.handlePlacement(block: CustomBlock, input: String, afterSeed: Function.() -> Unit = {}) {
	placementGuard(block, checkExisting = false)
	placeBlock(block)
	setOnNew(block, "data.properties.input1", "\"$input\"")
	setOnNew(block, "data.properties.output1", "\"^ ^ ^1\"")
	afterSeed()
	finishPlacement(block, removePlaceTag = false, withinBlock = false)
}
