# /ra_interactive:blocks/infinite_lava_cauldron/tick
# Tick all infinite lava cauldrons

# Check for break detection (only if block is air)
execute as @e[tag=ra.custom_block.infinite_lava_cauldron] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_lava_cauldron] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:cauldron"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_lava_cauldron] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:cauldron","minecraft:item_name":'Infinite Lava Cauldron',"minecraft:custom_data":{ra:{infinite_lava_cauldron:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.infinite_lava_cauldron"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_lava_cauldron] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_lava_cauldron] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_lava_cauldron] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.infinite_lava_cauldron] remove ra.broken

# When cauldron is empty, refill with lava
execute as @e[tag=ra.custom_block.infinite_lava_cauldron] at @s if block ~ ~ ~ cauldron run setblock ~ ~ ~ lava_cauldron
