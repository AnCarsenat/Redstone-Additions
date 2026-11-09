package ra.example

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import ra.api.MultiblockSpec
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.item.ItemSpec
import ra.core.moduleLoaded
import ra.core.raw
import ra.core.ref
import ra.modules.multiblock.MultiblockModule
import ra.modules.multiblock.blastforge.BlastForge
import ra.modules.multiblock.blocks.BaseTier
import ra.modules.multiblock.blocks.Bases
import ra.multiblocklib.spec.Compiled

/**
 * Example addon (`ra_example`): the worked example behind docs/extension-examples.md.
 *
 * TEMPLATE -- intentionally not registered. Nothing calls ra_example:load or ra_example:tick, and
 * none of the example multiblocks appear in the #ra_lib_multiblock:* tags, so this namespace never
 * runs in game. To make it live: call ra_example:load from ra:load and ra_example:tick from
 * ra:tick, and hook the example's five functions per multiblock into the #ra_lib_multiblock tags.
 * Its placement handlers are likewise not in #ra:placement_handlers.
 */
object ExamplePart : PackPart {
	private val ns = Ns.EXAMPLE

	/** An example base: its own item, placed as a Multiblock Base of [tier]. */
	private class ExampleBase(val dir: String, val tier: BaseTier, val model: String, val name: String, val lore: String) {
		val customData get() = "example_$dir"
		val placeTag get() = "ra.place.example.$dir"

		/** The item, once: given by `give`, and refunded when its placement is refused. */
		val item get() = ItemSpec.fromSnbt(
			"minecraft:bat_spawn_egg",
			"item_model" to "\"$model\"",
			"item_name" to "\"$name\"",
			"lore" to """[{text:"$lore",color:"gray",italic:false}]""",
			"custom_data" to "{ra:{$customData:1b}}",
			"entity_data" to """{id:"minecraft:bat",Tags:["ra","ra.spawned","$placeTag"],Silent:1b,NoAI:1b,Invulnerable:1b}""",
		)
	}

	private val bases = listOf(
		ExampleBase("copper_base", BaseTier.all.first { it.tier == "copper" }, "minecraft:blast_furnace", "Blast Forge Base", "Example multiblock core for the blast forge"),
	)

	override fun Ra.register() {
		pack.define(ref(ns, "load")) {
			function(MultiblockModule.load)
			moduleLoaded("Example multiblocks loaded!", "aqua")
		}
		pack.define(ref(ns, "tick")) { function(MultiblockModule.tick!!) }

		for (b in bases) {
			pack.define(ref(ns, "blocks/${b.dir}/give")) {
				raw("give @s ${b.item.giveArg()}")
			}
			extraPlaceables += b.item.stackNbt()
			// As the bat: borrows the tier's place tag for the duration of the base's own handler.
			pack.define(ref(ns, "blocks/${b.dir}/handle_placement")) {
				raw("execute unless entity @s[tag=${b.placeTag}] run return 0")
				val tierPlace = "ra.place.${b.tier.blockTag}"
				tag(self()) { add(tierPlace) }
				function(Bases.baseHandlePlacement, Storage.MULTIBLOCK, b.tier.path)
				tag(self()) { remove(tierPlace) }
				returnValue(1)
			}
		}

		pack.define(ref(ns, "items/give_all")) {
			for (b in bases) function(ref(ns, "blocks/${b.dir}/give"))
		}

		// The example multiblock's hooks, each delegating to the real ra_multiblock function.
		// It is a compiled spec, so the hook bodies are the library's generated functions, each
		// guarded the way a hook tag entry must be (it runs for every type).
		hooks("blast_forge", BlastForge.SPEC, BlastForge.tickDispatch)
	}

	private fun Ra.hooks(dir: String, spec: MultiblockSpec, tickDispatch: FunctionArgument) {
		pack.define(ref(ns, "$dir/check_structure")) {
			raw("execute if entity @s[tag=${spec.tag}] run function ${Compiled.checkOf(spec).asId()}")
		}
		// The structure needs no cleanup: nothing to do on break.
		pack.define(ref(ns, "$dir/on_break")) { raw("return 0") }
		pack.define(ref(ns, "$dir/setup_type")) {
			raw("""execute if data entity @s data{type:"${spec.id}"} run tag @s add ${spec.tag}""")
		}
		pack.define(ref(ns, "$dir/tick_dispatch")) { function(tickDispatch) }
		pack.define(ref(ns, "$dir/validate")) {
			raw("""execute if data storage ra:multiblock {type:"${spec.id}"} run function ${Compiled.validateOf(spec).asId()}""")
		}
	}
}
