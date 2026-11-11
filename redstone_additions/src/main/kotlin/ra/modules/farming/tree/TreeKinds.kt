package ra.modules.farming.tree

/**
 * The trees a [TreeFarm] grows: the ones a single sapling grows into, with one trunk standing on
 * the sapling's block. Dark oak and pale oak need four saplings, mangroves raise their trunk on
 * roots, and the big spruce and jungle trees need four saplings too, so none of those is here.
 *
 * [index] is what `data.data.kind` stores: never reorder or reuse one, add new kinds at the end.
 * [features] are the `place feature` ids bone meal grows, one picked at random (a repeated id is
 * weight): an oak sapling gives a fancy oak one time in ten, as in vanilla.
 */
internal data class TreeKind(
	val index: Int,
	val name: String,
	val sapling: String,
	val log: String,
	val leaves: List<String>,
	val features: List<String>,
)

internal val TreeKinds = listOf(
	TreeKind(0, "oak", "minecraft:oak_sapling", "minecraft:oak_log", listOf("minecraft:oak_leaves"), List(9) { "minecraft:oak" } + "minecraft:fancy_oak"),
	TreeKind(1, "spruce", "minecraft:spruce_sapling", "minecraft:spruce_log", listOf("minecraft:spruce_leaves"), listOf("minecraft:spruce")),
	TreeKind(2, "birch", "minecraft:birch_sapling", "minecraft:birch_log", listOf("minecraft:birch_leaves"), listOf("minecraft:birch")),
	TreeKind(3, "jungle", "minecraft:jungle_sapling", "minecraft:jungle_log", listOf("minecraft:jungle_leaves"), listOf("minecraft:jungle_tree_no_vine")),
	TreeKind(4, "acacia", "minecraft:acacia_sapling", "minecraft:acacia_log", listOf("minecraft:acacia_leaves"), listOf("minecraft:acacia")),
	TreeKind(5, "cherry", "minecraft:cherry_sapling", "minecraft:cherry_log", listOf("minecraft:cherry_leaves"), listOf("minecraft:cherry")),
	TreeKind(
		6, "poplar", "minecraft:poplar_sapling", "minecraft:poplar_log",
		listOf("minecraft:orange_poplar_leaves", "minecraft:red_poplar_leaves", "minecraft:yellow_poplar_leaves"),
		listOf("minecraft:orange_poplar", "minecraft:red_poplar", "minecraft:yellow_poplar"),
	),
)
