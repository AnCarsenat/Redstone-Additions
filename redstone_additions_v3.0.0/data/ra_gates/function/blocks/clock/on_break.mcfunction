# /ra_gates:blocks/clock/on_break
# Handle Clock gate break

# Kill vanilla daylight_detector drop
kill @e[type=item,nbt={Item:{id:"minecraft:daylight_detector"}},distance=..2,limit=1,sort=nearest]

# Kill display item
kill @e[type=item_display,tag=ra.custom_block.clock,distance=..2]
kill @e[type=item_display,tag=ra.custom_block.display_item.clock,distance=..2]

# Revert any redstone blocks back to iron blocks
fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# Drop the Clock item at the armor stand's position
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:daylight_detector","minecraft:item_name":"Clock","minecraft:custom_data":{ra:{clock:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.clock"],Silent:1b,NoAI:1b,Invulnerable:1b}}},Tags:["ra.drop_temp"]}

# Kill the armor stand
kill @s
