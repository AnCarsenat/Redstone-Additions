package ra.modules.railworks.carts

import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.railworks.carts.RailBuilder.BUILT
import ra.modules.railworks.carts.RailBuilder.CLEAR
import ra.modules.railworks.carts.RailBuilder.DIR
import ra.modules.railworks.carts.RailBuilder.Dir
import ra.modules.railworks.carts.RailBuilder.FILLER
import ra.modules.railworks.carts.RailBuilder.FLOOR
import ra.modules.railworks.carts.RailBuilder.LIQUID
import ra.modules.railworks.carts.RailBuilder.NOT_WALL
import ra.modules.railworks.carts.RailBuilder.SELF
import ra.modules.railworks.carts.RailBuilder.STEPS
import ra.modules.railworks.carts.RailBuilder.TORCH_EVERY
import ra.modules.railworks.carts.RailBuilder.UNBREAKABLE

/**
 * One block of a [RailBuilder]'s track. As the builder, positioned 0.2 above it (so that `~ ~ ~`
 * is the block its rail is in). F is the block ahead, H the one above it, B the one below.
 *
 * 1. A straight rail along the line already at F: just move onto it. Any other rail: stop.
 * 2. Stop ([REASONS]) when F is in an unloaded chunk (or the block after it is), when F or H can
 *    not be broken (`#minecraft:wither_immune`, portals, a block entity such as a chest or a
 *    spawner, a Redstone Additions block; the same for B when it needs filling), when F or H is a liquid, when B is the void, when it has
 *    no rail, or no filler while B needs one.
 * 3. Dig F, then H: their drops as a diamond pickaxe gets them (`loot ... mine`), put onto the
 *    builder, which picks them up. The block is removed after the loot is made, and the loot is
 *    thrown away again if the block could not be removed, so nothing is doubled.
 * 4. B is replaceable (air, a liquid, grass, snow...): put a filler block there, taken from the
 *    inventory one at a time ([FILLER]; the block placed is the item taken, by id).
 * 5. Every [TORCH_EVERY] blocks, with a redstone torch: hang it on the right wall at H, or the left
 *    one; with a powered rail too, F gets the powered rail, powered by that torch.
 * 6. Lay the rail at F, take it from the inventory, move onto it.
 *
 * Everything placed is taken from the inventory only once the block is really there.
 */
internal object RailBuilderStep {
	private const val T = Obj.TEMP

	/** #rb.why -> what the players near it are told. */
	val REASONS = listOf(
		1 to "the track ahead is not straight",
		2 to "the way ahead is not loaded",
		3 to "a block it may not break",
		4 to "liquid ahead",
		5 to "out of rails",
		6 to "out of filler blocks",
		7 to "nothing to build on",
		8 to "a block it could not clear",
	)

	private fun fn(path: String) = ref(Ns.RAILWORKS, "carts/builder/$path")

	val step = fn("step")
	private val mine = fn("mine")
	private val settle = fn("settle")
	private val fill = fn("fill")
	private val placeFiller = fn("place_filler")
	private val takeRail = fn("take/rail")
	private val takePowered = fn("take/powered_rail")
	private val takeTorch = fn("take/redstone_torch")

	/** Hopper minecart slots. */
	private val SLOTS = 0..4

	private fun stopWith(code: Int) = fn("stop_$code")

	fun register(ra: Ra) = with(ra) {
		for ((code, _) in REASONS) pack.define(stopWith(code)) {
			raw("scoreboard players set #rb.why $T $code")
			raw("function ${RailBuilder.stop.asId()}")
		}

		pack.define(step) {
			for (d in Dir.ALL) raw("execute if score @s $DIR matches ${d.code} run return run function ${fn("step_${d.name}").asId()}")
		}
		for (d in Dir.ALL) {
			val torch = fn("torch_${d.name}")
			val advance = fn("advance_${d.name}")
			pack.define(fn("step_${d.name}")) { stepFor(d, torch, advance) }
			pack.define(torch) { torchFor(d) }
			pack.define(advance) {
				raw("execute at @s run tp @s ${d.at(1, 0)}")
				raw("data modify entity @s Motion set value [0.0d,0.0d,0.0d]")
			}
		}

		// As the builder, at the centre of the block to dig. Drops onto the builder.
		pack.define(mine) {
			raw("tag @s add $SELF")
			raw("loot spawn ~ ~ ~ mine ~ ~ ~ minecraft:diamond_pickaxe")
			raw("tag @e[type=minecraft:item,distance=..0.01] add ra.rb.loot")
			raw("scoreboard players set #rb.mined $T 0")
			raw("execute store success score #rb.mined $T run setblock ~ ~ ~ minecraft:air")
			raw("execute if score #rb.mined $T matches 0 run kill @e[type=minecraft:item,tag=ra.rb.loot,distance=..0.01]")
			raw("execute as @e[type=minecraft:item,tag=ra.rb.loot,distance=..0.01] run function ${settle.asId()}")
			raw("tag @s remove $SELF")
		}
		pack.define(settle) {
			raw("tp @s @n[type=minecraft:hopper_minecart,tag=$SELF]")
			raw("data modify entity @s Motion set value [0.0d,0.0d,0.0d]")
			raw("tag @s remove ra.rb.loot")
		}

		// As the builder, at the centre of B. #rb.ok: 1 when a filler block is there now.
		pack.define(fill) {
			raw("scoreboard players set #rb.ok $T 0")
			raw("execute unless block ~ ~ ~ $CLEAR unless block ~ ~ ~ $LIQUID run function ${mine.asId()}")
			raw("scoreboard players set #rb.slot $T -1")
			for (n in SLOTS) raw("execute if score #rb.slot $T matches -1 if items entity @s container.$n $FILLER run scoreboard players set #rb.slot $T $n")
			raw("execute if score #rb.slot $T matches -1 run return 0")
			raw("data remove storage ra:temp rb")
			for (n in SLOTS) raw("execute if score #rb.slot $T matches $n run data modify storage ra:temp rb.id set from entity @s Items[{Slot:${n}b}].id")
			raw("execute if data storage ra:temp rb.id run function ${placeFiller.asId()} with storage ra:temp rb")
			for (n in SLOTS) raw("""execute if score #rb.ok $T matches 1 if score #rb.slot $T matches $n run item modify entity @s container.$n {type:"minecraft:set_count",count:-1,add:true}""")
			raw("data remove storage ra:temp rb")
		}
		// {id}: an item id from the inventory that is in #ra_railworks:builder_filler, so a block id.
		pack.define(placeFiller) {
			raw("\$execute store success score #rb.ok $T run setblock ~ ~ ~ \$(id)")
		}

		for ((f, item) in listOf(takeRail to "minecraft:rail", takePowered to "minecraft:powered_rail", takeTorch to "minecraft:redstone_torch")) pack.define(f) {
			raw("scoreboard players set #rb.took $T 0")
			for (n in SLOTS) raw("""execute if score #rb.took $T matches 0 if items entity @s container.$n $item store success score #rb.took $T run item modify entity @s container.$n {type:"minecraft:set_count",count:-1,add:true}""")
		}
	}

	private fun Function.why(code: Int, cond: String) = raw("execute $cond run return run function ${stopWith(code).asId()}")

	/** Stop (3) when [p] is a block it may not break: unbreakable, a block entity, a Redstone Additions block. */
	private fun Function.protectedBlock(guard: String, p: String) {
		why(3, "${guard}if block $p $UNBREAKABLE")
		why(3, "${guard}if data block $p id")
		for (tag in listOf(Tag.CUSTOM_BLOCK, "ra.multiblock"))
			why(3, "${guard}positioned $p align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[type=minecraft:marker,tag=$tag,distance=..0.6]")
	}

	private fun Function.stepFor(d: Dir, torch: io.github.ayfri.kore.arguments.types.resources.FunctionArgument, advance: io.github.ayfri.kore.arguments.types.resources.FunctionArgument) {
		val f = d.at(1, 0)
		val h = d.at(1, 1)
		val b = d.at(1, -1)
		raw("execute if block $f #minecraft:rails[shape=${d.axis}] run return run function ${advance.asId()}")
		why(1, "if block $f #minecraft:rails")
		why(2, "unless loaded $f")
		why(2, "unless loaded ${d.at(2, 0)}")
		why(7, "if block $b minecraft:void_air")
		for (p in listOf(f, h)) {
			why(4, "if block $p $LIQUID")
			protectedBlock("", p)
		}
		raw("scoreboard players set #rb.fl $T 0")
		raw("execute if block $b $FLOOR run scoreboard players set #rb.fl $T 1")
		// B is only replaced when it needs filling, and then with the same guards as F and H.
		protectedBlock("if score #rb.fl $T matches 1 ", b)
		why(5, "unless items entity @s container.* minecraft:rail")
		why(6, "if score #rb.fl $T matches 1 unless items entity @s container.* $FILLER")

		// F first: digging the lower half of a door or a tall plant drops it whole.
		for (p in listOf(f, h)) raw("execute unless block $p $CLEAR positioned $p align xyz positioned ~0.5 ~0.5 ~0.5 run function ${mine.asId()}")
		why(8, "unless block $f $CLEAR")
		why(8, "unless block $h $CLEAR")

		raw("execute if score #rb.fl $T matches 1 positioned $b align xyz positioned ~0.5 ~0.5 ~0.5 run function ${fill.asId()}")
		why(6, "if score #rb.fl $T matches 1 if score #rb.ok $T matches 0")

		raw("scoreboard players set #rb.torch $T 0")
		raw("execute if score @s $STEPS matches ${TORCH_EVERY - 1}.. if items entity @s container.* minecraft:redstone_torch run function ${torch.asId()}")

		raw("scoreboard players set #rb.pw $T 0")
		raw("execute if score #rb.torch $T matches 1 if items entity @s container.* minecraft:powered_rail run scoreboard players set #rb.pw $T 1")
		raw("scoreboard players set #rb.ok $T 0")
		raw("execute if score #rb.pw $T matches 1 store success score #rb.ok $T run setblock $f minecraft:powered_rail[shape=${d.axis},powered=true]")
		raw("execute if score #rb.pw $T matches 0 store success score #rb.ok $T run setblock $f minecraft:rail[shape=${d.axis}]")
		raw("execute if score #rb.ok $T matches 1 if score #rb.pw $T matches 1 run function ${takePowered.asId()}")
		raw("execute if score #rb.ok $T matches 1 if score #rb.pw $T matches 0 run function ${takeRail.asId()}")
		why(8, "unless block $f #minecraft:rails")

		raw("scoreboard players add @s $STEPS 1")
		raw("execute if score #rb.torch $T matches 1 run scoreboard players set @s $STEPS 0")
		raw("scoreboard players add @s $BUILT 1")
		raw("function ${advance.asId()}")
		raw("playsound minecraft:block.stone.break block ${ra.core.block.soundListenersSel(16)} ~ ~ ~ 0.4 0.9")
	}

	/** Hang a redstone torch at H on the right wall, else the left. #rb.torch = 1 when one is there. */
	private fun Function.torchFor(d: Dir) {
		val h = d.at(1, 1)
		val right = Dir.ALL.first { it.dx == d.rx && it.dz == d.rz }.name
		val left = Dir.ALL.first { it.dx == -d.rx && it.dz == -d.rz }.name
		raw("execute if block $h $CLEAR unless block ${d.at(1, 1, 1)} $NOT_WALL store success score #rb.torch $T run setblock $h minecraft:redstone_wall_torch[facing=$left]")
		raw("execute if score #rb.torch $T matches 0 if block $h $CLEAR unless block ${d.at(1, 1, -1)} $NOT_WALL store success score #rb.torch $T run setblock $h minecraft:redstone_wall_torch[facing=$right]")
		raw("execute if score #rb.torch $T matches 1 run function ${takeTorch.asId()}")
	}
}
