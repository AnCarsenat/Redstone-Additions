package ra.modules.storage

import io.github.ayfri.kore.functions.Function
import ra.core.block.CustomBlock
import ra.core.block.defaultProperty
import ra.core.block.finishPlacement
import ra.core.block.markerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.raw

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

/**
 * Tick-time IO upkeep, shared by the Boxer and Unboxer: default `input1` / `output1` where missing,
 * and the one-time move of the old up/down defaults to rear/front.
 */
internal fun Function.ioDefaults(block: CustomBlock, input: String) {
	// IO defaults the macro processing needs.
	defaultProperty(block, "input1", "\"$input\"")
	defaultProperty(block, "output1", "\"^ ^ ^1\"")

	// Migrate legacy up/down defaults to rear/front defaults.
	val migrated = "ra.storage.io_default_migrated"
	raw("execute as ${block.markerSel("tag=!$migrated")} if data entity @s data.properties{input1:\"^ ^-1 ^\",output1:\"^ ^1 ^\"} run data merge entity @s {data:{properties:{input1:\"^ ^ ^-1\",output1:\"^ ^ ^1\"}}}")
	raw("tag ${block.markerSel("tag=!$migrated")} add $migrated")
}
