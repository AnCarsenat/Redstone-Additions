# /ra_wires:liquid/break/gas_tank
# Clean up a broken gas tank and return its item.
# Context: as the marker, at its position. Ends by killing the marker.

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
kill @e[type=item,nbt={Item:{id:"minecraft:iron_block"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:iron_block","minecraft:item_name":'Gas Tank',"minecraft:custom_data":{ra:{gas_tank:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.gas_tank"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
