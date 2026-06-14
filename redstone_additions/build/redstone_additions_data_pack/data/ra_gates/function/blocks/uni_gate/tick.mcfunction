# /ra_gates:blocks/uni_gate/tick
# Tick all UNI gates

# Check for break detection
execute as @e[tag=ra.custom_block.uni_gate] at @s unless block ~ ~ ~ smooth_stone_slab run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.uni_gate] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:smooth_stone_slab"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.uni_gate] at @s run kill @e[type=item_display,tag=ra.custom_block.uni_gate,distance=..2]
execute as @e[tag=ra.broken,tag=ra.custom_block.uni_gate] at @s run kill @e[type=item_display,tag=ra.custom_block.display_item.uni_gate,distance=..2]
execute as @e[tag=ra.broken,tag=ra.custom_block.uni_gate] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute as @e[tag=ra.broken,tag=ra.custom_block.uni_gate] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:smooth_stone_slab","minecraft:item_name":"UNI Gate","minecraft:custom_data":{ra:{uni_gate:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.uni_gate"],Silent:1b,NoAI:1b,Invulnerable:1b}}},Tags:["ra.drop_temp"]}
execute as @e[tag=ra.broken,tag=ra.custom_block.uni_gate] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.uni_gate] remove ra.broken

# Process gate logic
execute as @e[tag=ra.custom_block.uni_gate] at @s run function ra_gates:blocks/uni_gate/process
