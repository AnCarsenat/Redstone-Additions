# /ra_gates:blocks/clock/tick
# Tick all clocks

# Check for break detection
execute as @e[tag=ra.custom_block.clock] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.clock] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:daylight_detector"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.clock] at @s run kill @e[type=item_display,tag=ra.custom_block.clock,distance=..2]
execute as @e[tag=ra.broken,tag=ra.custom_block.clock] at @s run kill @e[type=item_display,tag=ra.custom_block.display_item.clock,distance=..2]
execute as @e[tag=ra.broken,tag=ra.custom_block.clock] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute as @e[tag=ra.broken,tag=ra.custom_block.clock] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:daylight_detector","minecraft:item_name":"Clock","minecraft:custom_data":{ra:{clock:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.clock"],Silent:1b,NoAI:1b,Invulnerable:1b}}},Tags:["ra.drop_temp"]}
execute as @e[tag=ra.broken,tag=ra.custom_block.clock] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.clock] remove ra.broken

# Process gate logic
execute as @e[tag=ra.custom_block.clock] at @s run function ra_gates:blocks/clock/process with entity @s data.properties


