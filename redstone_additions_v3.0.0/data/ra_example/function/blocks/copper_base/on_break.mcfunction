# /ra_example:blocks/copper_base/on_break
# Cleanup for example copper base

kill @e[type=item,nbt={Item:{id:"minecraft:blast_furnace"}},distance=..2,limit=1,sort=nearest]
kill @e[type=block_display,tag=ra.custom_block.multiblock_base.copper,distance=..2,limit=1,sort=nearest]
execute align xyz positioned ~0.5 ~ ~0.5 as @e[tag=ra.multiblock,distance=..0.5,limit=1,sort=nearest] run function ra_lib_multiblock:disassemble

summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blast_furnace","minecraft:item_name":"Blast Forge Base","minecraft:lore":[{text:"Example multiblock core for the blast forge",color:"gray",italic:false}],"minecraft:custom_data":{ra:{example_copper_base:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.example.copper_base"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

playsound minecraft:block.copper.break block @a[distance=..16] ~ ~ ~ 1 0.8
particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5

kill @s
