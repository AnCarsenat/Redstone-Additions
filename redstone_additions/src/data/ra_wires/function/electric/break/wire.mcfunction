# /ra_wires:electric/break/wire
# Clean up a broken wire and return its item.
# Context: as the marker, at its position. Ends by killing the marker.

kill @e[type=block_display,tag=ra.wires.wire_display,distance=..0.9]
# Stop advertising this node BEFORE the neighbours redraw. update_model_local_and_neighbors
# asks each neighbour to look around, and their connection test finds node markers —
# so while this marker still carried its node tags, every neighbour redrew a
# connector pointing at a pipe that had already been destroyed. That is the
# connection that was left dangling next to a broken block.
tag @s remove ra.wires.fluid_node
tag @s remove ra.wires.electric_node
tag @s remove ra.wires.node
function ra_lib:transport/net/leave
function ra_wires:common/update_model_local_and_neighbors
kill @e[type=item,nbt={Item:{id:"minecraft:conduit"}},distance=..2,limit=2]
kill @e[type=item,nbt={Item:{id:"minecraft:mud_brick_wall"}},distance=..2,limit=2]
kill @e[type=item,nbt={Item:{id:"minecraft:polished_blackstone_wall"}},distance=..2,limit=2]
execute if data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:black_candle","minecraft:item_name":'L2 Wire',"minecraft:custom_data":{ra:{electric_wire_netherite:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_wire_netherite"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute unless data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:orange_candle","minecraft:item_name":'Copper Wire',"minecraft:custom_data":{ra:{electric_wire_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_wire_copper"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
