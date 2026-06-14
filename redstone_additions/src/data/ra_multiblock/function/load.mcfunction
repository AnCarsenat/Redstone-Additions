# /ra_multiblock:load
# Load multiblock module - register multiblock type data

tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Multiblock module loaded",color:"gray"}]

# Heat scoreboard for blast forge
scoreboard objectives add ra.heat dummy

# ============================================================================
# Blast Forge - Direction Data
# All block positions relative to base (0,0,0) for each facing direction
# Keys: furnace, in1, in2, out1, ctrl, smoke (string coords for macros)
#       b1-b4 (corner bricks), f1-f9 (floor), c1-c9 (ceiling)
#       io_inputs, io_outputs, io_controls (compound metadata for entity)
# ============================================================================

data modify storage ra:multiblock bf_dir.north set value {facing:"north",furnace:"~ ~ ~-1",in1:"~-1 ~ ~-1",in2:"~1 ~ ~-1",out1:"~ ~ ~-2",ctrl:"~ ~ ~1",smoke:"~ ~0.5 ~-1",b1:"~-1 ~ ~0",b2:"~1 ~ ~0",b3:"~-1 ~ ~-2",b4:"~1 ~ ~-2",f1:"~-1 ~-1 ~-2",f2:"~0 ~-1 ~-2",f3:"~1 ~-1 ~-2",f4:"~-1 ~-1 ~-1",f5:"~0 ~-1 ~-1",f6:"~1 ~-1 ~-1",f7:"~-1 ~-1 ~0",f8:"~0 ~-1 ~0",f9:"~1 ~-1 ~0",c1:"~-1 ~1 ~-2",c2:"~0 ~1 ~-2",c3:"~1 ~1 ~-2",c4:"~-1 ~1 ~-1",c5:"~0 ~1 ~-1",c6:"~1 ~1 ~-1",c7:"~-1 ~1 ~0",c8:"~0 ~1 ~0",c9:"~1 ~1 ~0",io_inputs:{input_1:{x:-1,y:0,z:-1},input_2:{x:1,y:0,z:-1}},io_outputs:{output_1:{x:0,y:0,z:-2}},io_controls:{redstone_in:{x:0,y:0,z:1}}}

data modify storage ra:multiblock bf_dir.south set value {facing:"south",furnace:"~ ~ ~1",in1:"~1 ~ ~1",in2:"~-1 ~ ~1",out1:"~ ~ ~2",ctrl:"~ ~ ~-1",smoke:"~ ~0.5 ~1",b1:"~-1 ~ ~0",b2:"~1 ~ ~0",b3:"~-1 ~ ~2",b4:"~1 ~ ~2",f1:"~-1 ~-1 ~0",f2:"~0 ~-1 ~0",f3:"~1 ~-1 ~0",f4:"~-1 ~-1 ~1",f5:"~0 ~-1 ~1",f6:"~1 ~-1 ~1",f7:"~-1 ~-1 ~2",f8:"~0 ~-1 ~2",f9:"~1 ~-1 ~2",c1:"~-1 ~1 ~0",c2:"~0 ~1 ~0",c3:"~1 ~1 ~0",c4:"~-1 ~1 ~1",c5:"~0 ~1 ~1",c6:"~1 ~1 ~1",c7:"~-1 ~1 ~2",c8:"~0 ~1 ~2",c9:"~1 ~1 ~2",io_inputs:{input_1:{x:1,y:0,z:1},input_2:{x:-1,y:0,z:1}},io_outputs:{output_1:{x:0,y:0,z:2}},io_controls:{redstone_in:{x:0,y:0,z:-1}}}

data modify storage ra:multiblock bf_dir.east set value {facing:"east",furnace:"~1 ~ ~",in1:"~1 ~ ~-1",in2:"~1 ~ ~1",out1:"~2 ~ ~",ctrl:"~-1 ~ ~",smoke:"~1 ~0.5 ~",b1:"~0 ~ ~-1",b2:"~0 ~ ~1",b3:"~2 ~ ~-1",b4:"~2 ~ ~1",f1:"~0 ~-1 ~-1",f2:"~0 ~-1 ~0",f3:"~0 ~-1 ~1",f4:"~1 ~-1 ~-1",f5:"~1 ~-1 ~0",f6:"~1 ~-1 ~1",f7:"~2 ~-1 ~-1",f8:"~2 ~-1 ~0",f9:"~2 ~-1 ~1",c1:"~0 ~1 ~-1",c2:"~0 ~1 ~0",c3:"~0 ~1 ~1",c4:"~1 ~1 ~-1",c5:"~1 ~1 ~0",c6:"~1 ~1 ~1",c7:"~2 ~1 ~-1",c8:"~2 ~1 ~0",c9:"~2 ~1 ~1",io_inputs:{input_1:{x:1,y:0,z:-1},input_2:{x:1,y:0,z:1}},io_outputs:{output_1:{x:2,y:0,z:0}},io_controls:{redstone_in:{x:-1,y:0,z:0}}}

data modify storage ra:multiblock bf_dir.west set value {facing:"west",furnace:"~-1 ~ ~",in1:"~-1 ~ ~1",in2:"~-1 ~ ~-1",out1:"~-2 ~ ~",ctrl:"~1 ~ ~",smoke:"~-1 ~0.5 ~",b1:"~0 ~ ~-1",b2:"~0 ~ ~1",b3:"~-2 ~ ~-1",b4:"~-2 ~ ~1",f1:"~-2 ~-1 ~-1",f2:"~-2 ~-1 ~0",f3:"~-2 ~-1 ~1",f4:"~-1 ~-1 ~-1",f5:"~-1 ~-1 ~0",f6:"~-1 ~-1 ~1",f7:"~0 ~-1 ~-1",f8:"~0 ~-1 ~0",f9:"~0 ~-1 ~1",c1:"~-2 ~1 ~-1",c2:"~-2 ~1 ~0",c3:"~-2 ~1 ~1",c4:"~-1 ~1 ~-1",c5:"~-1 ~1 ~0",c6:"~-1 ~1 ~1",c7:"~0 ~1 ~-1",c8:"~0 ~1 ~0",c9:"~0 ~1 ~1",io_inputs:{input_1:{x:-1,y:0,z:1},input_2:{x:-1,y:0,z:-1}},io_outputs:{output_1:{x:-2,y:0,z:0}},io_controls:{redstone_in:{x:1,y:0,z:0}}}

# ============================================================================
# Multiblock Base Tier Data (for macro-based give/placement/on_break)
# ============================================================================

data modify storage ra:multiblock tier_data.copper set value {tier:"copper",tier_name:"Copper",tier_level:1,block_id:"minecraft:waxed_copper_block",block_model:"minecraft:waxed_copper_block",block_tag:"multiblock_base.copper",lore_text:"Tier 1 Multiblock Core",break_sound:"minecraft:block.copper.break",break_volume:"1",break_pitch:"0.8",place_sound:"minecraft:block.copper.place",place_volume:"1",place_pitch:"0.8",display_block:"minecraft:blast_furnace",display_scale:0.65}

data modify storage ra:multiblock tier_data.iron set value {tier:"iron",tier_name:"Iron",tier_level:2,block_id:"minecraft:iron_block",block_model:"minecraft:iron_block",block_tag:"multiblock_base.iron",lore_text:"Tier 2 Multiblock Core",break_sound:"minecraft:block.metal.break",break_volume:"1",break_pitch:"1",place_sound:"minecraft:block.anvil.land",place_volume:"0.5",place_pitch:"1.2",display_block:"minecraft:smithing_table",display_scale:0.65}

data modify storage ra:multiblock tier_data.gold set value {tier:"gold",tier_name:"Gold",tier_level:3,block_id:"minecraft:gold_block",block_model:"minecraft:gold_block",block_tag:"multiblock_base.gold",lore_text:"Tier 3 Multiblock Core",break_sound:"minecraft:block.metal.break",break_volume:"1",break_pitch:"0.8",place_sound:"minecraft:entity.experience_orb.pickup",place_volume:"0.5",place_pitch:"0.8",display_block:"minecraft:gold_block",display_scale:0.65}

data modify storage ra:multiblock tier_data.diamond set value {tier:"diamond",tier_name:"Diamond",tier_level:4,block_id:"minecraft:diamond_block",block_model:"minecraft:diamond_block",block_tag:"multiblock_base.diamond",lore_text:"Tier 4 Multiblock Core",break_sound:"minecraft:block.amethyst_block.break",break_volume:"1",break_pitch:"1",place_sound:"minecraft:block.amethyst_block.chime",place_volume:"1",place_pitch:"1.2",display_block:"minecraft:diamond_block",display_scale:0.65}

data modify storage ra:multiblock tier_data.netherite set value {tier:"netherite",tier_name:"Netherite",tier_level:5,block_id:"minecraft:netherite_block",block_model:"minecraft:netherite_block",block_tag:"multiblock_base.netherite",lore_text:"Tier 5 Multiblock Core",break_sound:"minecraft:block.netherite_block.break",break_volume:"0.8",break_pitch:"0.6",place_sound:"minecraft:block.respawn_anchor.charge",place_volume:"0.8",place_pitch:"0.6",display_block:"minecraft:lodestone",display_scale:0.65}

# ============================================================================
# Upgrade Platform — No direction data needed (rotationally symmetric)
# Structure: 3x1x3 smooth stone platform with netherite base center
# Assembles via wrench on netherite multiblock base
# ============================================================================
