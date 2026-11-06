package ra.modules.wires.blocks.electric_furnace

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref

object ElectricFurnaceSettings : PackPart {
	val cycleMode = ref(Ns.WIRES, "blocks/electric_furnace/cycle_mode")
	val cycleOutput = ref(Ns.WIRES, "blocks/electric_furnace/cycle_output")
	val goggles = ref(Ns.WIRES, "blocks/electric_furnace/goggles")
	val initRecipes = ref(Ns.WIRES, "blocks/electric_furnace/init_recipes")
	val readMode = ref(Ns.WIRES, "blocks/electric_furnace/read_mode")
	val readOutput = ref(Ns.WIRES, "blocks/electric_furnace/read_output")

	override fun Ra.register() {
		pack.define(ElectricFurnaceSettings.cycleMode) {
			// Wrench/goggles action: step through the four power modes.
			// Context: as the marker, at the block.

			raw("execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value \"low\"")

			scoreboard.players.set(literal("#ef.m"), "ra.wires.tmp", 0)
			raw("""execute if data entity @s data.properties{mode:"low"} run scoreboard players set #ef.m ra.wires.tmp 1""")
			raw("""execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.m ra.wires.tmp 2""")
			raw("""execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.m ra.wires.tmp 3""")

			raw("execute if score #ef.m ra.wires.tmp matches 0 run data modify entity @s data.properties.mode set value \"low\"")
			raw("execute if score #ef.m ra.wires.tmp matches 1 run data modify entity @s data.properties.mode set value \"medium\"")
			raw("execute if score #ef.m ra.wires.tmp matches 2 run data modify entity @s data.properties.mode set value \"high\"")
			raw("execute if score #ef.m ra.wires.tmp matches 3 run data modify entity @s data.properties.mode set value \"superpowered\"")

			raw("""execute if score #ef.m ra.wires.tmp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"low - 100 ticks, 40 EU/item",color:"aqua"}]""")
			raw("""execute if score #ef.m ra.wires.tmp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"medium - 50 ticks, 100 EU/item",color:"green"}]""")
			raw("""execute if score #ef.m ra.wires.tmp matches 2 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"high - 20 ticks, 300 EU/item",color:"yellow"}]""")
			raw("""execute if score #ef.m ra.wires.tmp matches 3 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"superpowered - 5 ticks, 1000 EU/item",color:"red"}]""")

			raw("playsound minecraft:block.lever.click block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4")
		}

		pack.define(ElectricFurnaceSettings.cycleOutput) {
			// Wrench/goggles action: step the output face round under -> front -> back -> top.
			// Context: as the marker, at the block.
			//
			// One of three settings this block declares in ra:tools/wrench/init_registry, so
			// the wrench offers it in a menu alongside the power mode and the enable switch
			// rather than each living on a different tool.

			raw("execute unless data entity @s data.properties.output run data modify entity @s data.properties.output set value \"under\"")

			scoreboard.players.set(literal("#ef.o"), "ra.wires.tmp", 0)
			raw("""execute if data entity @s data.properties{output:"under"} run scoreboard players set #ef.o ra.wires.tmp 1""")
			raw("""execute if data entity @s data.properties{output:"front"} run scoreboard players set #ef.o ra.wires.tmp 2""")
			raw("""execute if data entity @s data.properties{output:"back"} run scoreboard players set #ef.o ra.wires.tmp 3""")

			raw("execute if score #ef.o ra.wires.tmp matches 0 run data modify entity @s data.properties.output set value \"under\"")
			raw("execute if score #ef.o ra.wires.tmp matches 1 run data modify entity @s data.properties.output set value \"front\"")
			raw("execute if score #ef.o ra.wires.tmp matches 2 run data modify entity @s data.properties.output set value \"back\"")
			raw("execute if score #ef.o ra.wires.tmp matches 3 run data modify entity @s data.properties.output set value \"top\"")

			// Wrench actions message everyone nearby. The wrench runs `as` the marker off a
			// raycast and never touches the player, so anything addressed to a player-side
			// tag would reach nobody -- which is exactly how this once cycled in silence.
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace output: ",color:"gray"},{nbt:"data.properties.output",entity:"@s",color:"green"}]""")
			raw("playsound minecraft:block.lever.click block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4")
		}

		pack.define(ElectricFurnaceSettings.goggles) {
			// Goggles readout, and the block's display name.
			// Context: as the block's marker, at the block position.

			raw("data modify storage ra:temp block_name set value \"Electric Furnace\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			function(Goggles.Billboard.stackReset, nbt { this["top"] = 80; this["step"] = 22 })
			function(Goggles.Billboard.stackedStatusLine, nbt { this["path"] = "state"; this["label"] = ""; this["color"] = "aqua"; this["suffix"] = "" })
			function(Goggles.Billboard.stackedStatusLine, nbt { this["path"] = "mode"; this["label"] = "Mode: "; this["color"] = "gold"; this["suffix"] = "" })
			function(Goggles.Billboard.stackedStatusLine, nbt { this["path"] = "draw"; this["label"] = "Draw: "; this["color"] = "yellow"; this["suffix"] = " EU/item" })
			function(Goggles.Billboard.stackedStatusLine, nbt { this["path"] = "output"; this["label"] = "Out: "; this["color"] = "green"; this["suffix"] = "" })
		}

		pack.define(ElectricFurnaceSettings.initRecipes) {
			// What the Electric Furnace can smelt, keyed by input item id.
			//
			// A data pack cannot ask the game what a vanilla smelting recipe produces, so
			// this is a hand-maintained table. Keyed rather than listed for the same reason
			// fuel_map is: the furnace's question is "is THIS thing smeltable?", asked once
			// per stack it looks at, not "which of ninety recipes is in there?".
			//
			// Deliberately NOT exhaustive in one direction: results that are themselves
			// smeltable are the thing to be careful with, because the furnace draws from and
			// returns to the same block when it has nowhere else to put things. Stone is the
			// one real chain here (cobblestone -> stone -> smooth_stone) and it is kept
			// because smooth stone is a thing people want; the input/output slot split is
			// what stops it running away.
			//
			// Unguarded, like every other registry in this pack, so an update to the table
			// reaches worlds that already exist.

			raw("""data modify storage ra:wires smelt_map set value {"minecraft:raw_iron":{result:"minecraft:iron_ingot"},"minecraft:raw_gold":{result:"minecraft:gold_ingot"},"minecraft:raw_copper":{result:"minecraft:copper_ingot"},"minecraft:iron_ore":{result:"minecraft:iron_ingot"},"minecraft:deepslate_iron_ore":{result:"minecraft:iron_ingot"},"minecraft:gold_ore":{result:"minecraft:gold_ingot"},"minecraft:deepslate_gold_ore":{result:"minecraft:gold_ingot"},"minecraft:copper_ore":{result:"minecraft:copper_ingot"},"minecraft:deepslate_copper_ore":{result:"minecraft:copper_ingot"},"minecraft:nether_gold_ore":{result:"minecraft:gold_ingot"},"minecraft:ancient_debris":{result:"minecraft:netherite_scrap"},"minecraft:coal_ore":{result:"minecraft:coal"},"minecraft:deepslate_coal_ore":{result:"minecraft:coal"},"minecraft:diamond_ore":{result:"minecraft:diamond"},"minecraft:deepslate_diamond_ore":{result:"minecraft:diamond"},"minecraft:emerald_ore":{result:"minecraft:emerald"},"minecraft:deepslate_emerald_ore":{result:"minecraft:emerald"},"minecraft:lapis_ore":{result:"minecraft:lapis_lazuli"},"minecraft:deepslate_lapis_ore":{result:"minecraft:lapis_lazuli"},"minecraft:redstone_ore":{result:"minecraft:redstone"},"minecraft:deepslate_redstone_ore":{result:"minecraft:redstone"},"minecraft:nether_quartz_ore":{result:"minecraft:quartz"},"minecraft:iron_sword":{result:"minecraft:iron_nugget"},"minecraft:iron_pickaxe":{result:"minecraft:iron_nugget"},"minecraft:iron_axe":{result:"minecraft:iron_nugget"},"minecraft:iron_shovel":{result:"minecraft:iron_nugget"},"minecraft:iron_hoe":{result:"minecraft:iron_nugget"},"minecraft:iron_helmet":{result:"minecraft:iron_nugget"},"minecraft:iron_chestplate":{result:"minecraft:iron_nugget"},"minecraft:iron_leggings":{result:"minecraft:iron_nugget"},"minecraft:iron_boots":{result:"minecraft:iron_nugget"},"minecraft:chainmail_helmet":{result:"minecraft:iron_nugget"},"minecraft:chainmail_chestplate":{result:"minecraft:iron_nugget"},"minecraft:chainmail_leggings":{result:"minecraft:iron_nugget"},"minecraft:chainmail_boots":{result:"minecraft:iron_nugget"},"minecraft:golden_sword":{result:"minecraft:gold_nugget"},"minecraft:golden_pickaxe":{result:"minecraft:gold_nugget"},"minecraft:golden_axe":{result:"minecraft:gold_nugget"},"minecraft:golden_shovel":{result:"minecraft:gold_nugget"},"minecraft:golden_hoe":{result:"minecraft:gold_nugget"},"minecraft:golden_helmet":{result:"minecraft:gold_nugget"},"minecraft:golden_chestplate":{result:"minecraft:gold_nugget"},"minecraft:golden_leggings":{result:"minecraft:gold_nugget"},"minecraft:golden_boots":{result:"minecraft:gold_nugget"},"minecraft:porkchop":{result:"minecraft:cooked_porkchop"},"minecraft:beef":{result:"minecraft:cooked_beef"},"minecraft:chicken":{result:"minecraft:cooked_chicken"},"minecraft:mutton":{result:"minecraft:cooked_mutton"},"minecraft:rabbit":{result:"minecraft:cooked_rabbit"},"minecraft:cod":{result:"minecraft:cooked_cod"},"minecraft:salmon":{result:"minecraft:cooked_salmon"},"minecraft:potato":{result:"minecraft:baked_potato"},"minecraft:kelp":{result:"minecraft:dried_kelp"},"minecraft:sand":{result:"minecraft:glass"},"minecraft:red_sand":{result:"minecraft:glass"},"minecraft:cobblestone":{result:"minecraft:stone"},"minecraft:cobbled_deepslate":{result:"minecraft:deepslate"},"minecraft:stone":{result:"minecraft:smooth_stone"},"minecraft:sandstone":{result:"minecraft:smooth_sandstone"},"minecraft:red_sandstone":{result:"minecraft:smooth_red_sandstone"},"minecraft:quartz_block":{result:"minecraft:smooth_quartz"},"minecraft:stone_bricks":{result:"minecraft:cracked_stone_bricks"},"minecraft:nether_bricks":{result:"minecraft:cracked_nether_bricks"},"minecraft:polished_blackstone_bricks":{result:"minecraft:cracked_polished_blackstone_bricks"},"minecraft:deepslate_bricks":{result:"minecraft:cracked_deepslate_bricks"},"minecraft:deepslate_tiles":{result:"minecraft:cracked_deepslate_tiles"},"minecraft:clay_ball":{result:"minecraft:brick"},"minecraft:clay":{result:"minecraft:terracotta"},"minecraft:netherrack":{result:"minecraft:nether_brick"},"minecraft:wet_sponge":{result:"minecraft:sponge"},"minecraft:sea_pickle":{result:"minecraft:lime_dye"},"minecraft:cactus":{result:"minecraft:green_dye"},"minecraft:chorus_fruit":{result:"minecraft:popped_chorus_fruit"},"minecraft:basalt":{result:"minecraft:smooth_basalt"},"minecraft:oak_log":{result:"minecraft:charcoal"},"minecraft:spruce_log":{result:"minecraft:charcoal"},"minecraft:birch_log":{result:"minecraft:charcoal"},"minecraft:jungle_log":{result:"minecraft:charcoal"},"minecraft:acacia_log":{result:"minecraft:charcoal"},"minecraft:dark_oak_log":{result:"minecraft:charcoal"},"minecraft:mangrove_log":{result:"minecraft:charcoal"},"minecraft:cherry_log":{result:"minecraft:charcoal"},"minecraft:pale_oak_log":{result:"minecraft:charcoal"},"minecraft:oak_wood":{result:"minecraft:charcoal"},"minecraft:spruce_wood":{result:"minecraft:charcoal"},"minecraft:birch_wood":{result:"minecraft:charcoal"}}""")
		}

		pack.define(ElectricFurnaceSettings.readMode) {
			// Turn the `mode` property into a speed and a cost.
			// Context: as the marker. Writes #ef.speed and #ef.cost in ra.wires.tmp.
			//
			// Ticks per item, EU per item, and what that works out at per tick:
			//
			// low            100 ticks    40 EU     0.4 EU/t   twice a vanilla furnace
			// medium          50 ticks    80 EU     1.6 EU/t
			// high            20 ticks   160 EU     8   EU/t
			// superpowered     5 ticks   300 EU    60   EU/t   forty times vanilla
			//
			// The EU per item climbs faster than the speed does on purpose. Going four times
			// quicker for four times the power would make the lower modes pointless -- there
			// would be no reason to ever run anything but superpowered. Paying more than
			// linearly for speed is what makes "low" a real choice when your grid is small.
			//
			// WHAT THE TOP MODE IS PRICED AGAINST
			// Superpowered used to cost 1000 EU an item, which is 200 EU/t -- three and a
			// third EU Generators running flat out, or about twenty Solar Panels once you
			// average a daylight cycle and cover the night. That is not a cost, it is a wall,
			// and it made the mode ornamental.
			//
			// The scale is now anchored on the generators the pack actually ships. One EU
			// Generator makes 60 EU/t, so superpowered is exactly one generator flat out --
			// a rule you can hold in your head while building. A Solar Panel peaks at 50
			// EU/t, so it is a couple of panels at noon and a handful plus a Battery across
			// a full day.
			//
			// The shape is unchanged: each step still costs more per unit of speed than the
			// one below it, and low is still much the cheapest way to smelt in bulk -- 7.5x
			// less EU per item than superpowered, for a twentieth of the rate.

			raw("execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value \"low\"")

			scoreboard.players.set(literal("#ef.speed"), "ra.wires.tmp", 100)
			scoreboard.players.set(literal("#ef.cost"), "ra.wires.tmp", 40)

			raw("""execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.speed ra.wires.tmp 50""")
			raw("""execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.cost ra.wires.tmp 80""")
			raw("""execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.speed ra.wires.tmp 20""")
			raw("""execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.cost ra.wires.tmp 160""")
			raw("""execute if data entity @s data.properties{mode:"superpowered"} run scoreboard players set #ef.speed ra.wires.tmp 5""")
			raw("""execute if data entity @s data.properties{mode:"superpowered"} run scoreboard players set #ef.cost ra.wires.tmp 300""")

			raw("data modify entity @s data.status.mode set from entity @s data.properties.mode")
			raw("execute store result entity @s data.status.draw int 1 run scoreboard players get #ef.cost ra.wires.tmp")
		}

		pack.define(ElectricFurnaceSettings.readOutput) {
			// Turn the `output` property into an offset in storage ra:wires ef.out.
			// Context: as the marker.
			//
			// ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. The furnace is
			// placed with dir 1, horizontal only, so front is always one of the four compass
			// directions and back is its opposite.
			//
			// Offsets rather than caret coordinates because carets depend on the marker's
			// Rotation, and a facing score is the thing the rest of the pack already trusts.

			raw("execute unless data entity @s data.properties.output run data modify entity @s data.properties.output set value \"under\"")

			raw("data modify storage ra:wires ef.out set value {dx:0,dy:-1,dz:0}")

			raw("""execute if data entity @s data.properties{output:"top"} run data modify storage ra:wires ef.out set value {dx:0,dy:1,dz:0}""")

			raw("""execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 2 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:-1}""")
			raw("""execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 3 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:1}""")
			raw("""execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 4 run data modify storage ra:wires ef.out set value {dx:-1,dy:0,dz:0}""")
			raw("""execute if data entity @s data.properties{output:"front"} if score @s ra.facing matches 5 run data modify storage ra:wires ef.out set value {dx:1,dy:0,dz:0}""")

			raw("""execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 2 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:1}""")
			raw("""execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 3 run data modify storage ra:wires ef.out set value {dx:0,dy:0,dz:-1}""")
			raw("""execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 4 run data modify storage ra:wires ef.out set value {dx:1,dy:0,dz:0}""")
			raw("""execute if data entity @s data.properties{output:"back"} if score @s ra.facing matches 5 run data modify storage ra:wires ef.out set value {dx:-1,dy:0,dz:0}""")

			raw("data modify entity @s data.status.output set from entity @s data.properties.output")
		}
	}
}
