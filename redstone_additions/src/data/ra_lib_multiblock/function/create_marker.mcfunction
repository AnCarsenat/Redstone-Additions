# /ra_lib_multiblock:create_marker
# Create multiblock marker entity at current position
# Context: at controller block position (multiblock base)
# Input storage ra:multiblock: {type:"multiblock_id", facing:"direction", inputs:{}, outputs:{}, controls:{}}

# Summon marker entity at block-aligned center
# NOTE: data:{_init:1b} ensures the compound exists — empty data:{} gets auto-removed by MC
execute align xyz run summon marker ~0.5 ~ ~0.5 {data:{_init:1b},Tags:["ra.multiblock","ra.multiblock.new"]}

# Transfer assembly data to marker entity (sort=nearest ensures correct entity)
execute align xyz positioned ~0.5 ~ ~0.5 as @e[tag=ra.multiblock.new,limit=1,sort=nearest] run function ra_lib_multiblock:setup_marker

# === Assembly Effects ===
playsound minecraft:block.respawn_anchor.set_spawn block @a[distance=..16] ~ ~ ~ 1 0.8
playsound minecraft:block.beacon.activate block @a[distance=..16] ~ ~ ~ 0.8 1.5
playsound minecraft:entity.player.levelup block @a[distance=..16] ~ ~ ~ 0.5 0.5

particle minecraft:totem_of_undying ~ ~1 ~ 0.5 0.5 0.5 0.3 50
particle minecraft:electric_spark ~ ~0.5 ~ 1 1 1 0.1 30
particle minecraft:entity_effect{color:[1.0,0.85,0.0,1.0]} ~ ~0.5 ~ 0.5 0.5 0.5 0 20

tellraw @a[distance=..16] [{text:"[RA] ",color:"gold"},{text:"Multiblock assembled!",color:"green",bold:true}]
