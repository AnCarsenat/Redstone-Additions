# /ra_interactive:blocks/infinite_water_cauldron/tick
# Tick all infinite water cauldrons

# Check for break detection (only if block is air)
execute as @e[tag=ra.custom_block.infinite_water_cauldron] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_water_cauldron] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:cauldron"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_water_cauldron] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:cauldron","minecraft:item_name":'Infinite Water Cauldron',"minecraft:custom_data":{ra:{infinite_water_cauldron:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.infinite_water_cauldron"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_water_cauldron] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_water_cauldron] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.infinite_water_cauldron] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.infinite_water_cauldron] remove ra.broken

# Refill cauldron if emptied or partially filled
execute as @e[tag=ra.custom_block.infinite_water_cauldron] at @s if block ~ ~ ~ cauldron run setblock ~ ~ ~ water_cauldron[level=3]
execute as @e[tag=ra.custom_block.infinite_water_cauldron] at @s if block ~ ~ ~ water_cauldron[level=1] run setblock ~ ~ ~ water_cauldron[level=3]
execute as @e[tag=ra.custom_block.infinite_water_cauldron] at @s if block ~ ~ ~ water_cauldron[level=2] run setblock ~ ~ ~ water_cauldron[level=3]
