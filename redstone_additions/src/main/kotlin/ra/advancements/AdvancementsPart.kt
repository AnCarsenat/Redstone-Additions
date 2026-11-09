package ra.advancements

import ra.core.PackPart
import ra.core.Ra

/**
 * The Redstone Additions advancement tab (`ra_advancements`): one root, a "core" node, and one
 * branch per module, each unlocked by holding the module's items. Purely informational; nothing
 * in the pack reads these advancements.
 *
 * The trees are data (see [coreTree] and the other lists); [unlock] turns each entry into its
 * advancement file.
 */
object AdvancementsPart : PackPart {
	private val trees = listOf(
		coreTree, exampleTree, gatesTree, sensorsTree, wirelessTree, interactiveTree,
		infiniteTree, jetpacksTree, wiresTree, enchantingTree, displaysTree,
	)

	override fun Ra.register() {
		for (tree in trees) for (entry in tree) pack.unlock(entry)
	}
}
