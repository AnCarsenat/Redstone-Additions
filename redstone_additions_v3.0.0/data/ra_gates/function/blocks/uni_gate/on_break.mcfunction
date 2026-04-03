# /ra_gates:blocks/uni_gate/on_break
# Handle UNI gate break

# Kill vanilla smooth_stone_slab drop
kill @e[type=item,nbt={Item:{id:"minecraft:smooth_stone_slab"}},distance=..2,limit=1,sort=nearest]

# Kill display item
kill @e[type=item_display,tag=ra.custom_block.uni_gate,distance=..2,limit=1,sort=nearest]

# Revert any redstone blocks back to iron blocks
fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# Drop the UNI Gate item at the armor stand's position
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:smooth_stone_slab","minecraft:item_name":"UNI Gate","minecraft:custom_data":{ra:{uni_gate:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.uni_gate"],Silent:1b,NoAI:1b,Invulnerable:1b}}},Tags:["ra.drop_temp"]}

# Kill the armor stand
kill @s
