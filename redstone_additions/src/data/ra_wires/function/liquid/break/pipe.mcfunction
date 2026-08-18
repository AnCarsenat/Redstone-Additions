# /ra_wires:liquid/break/pipe
# Clean up a broken pipe and return its item.
# Context: as the marker, at its position. Ends by killing the marker.
#
# only ever writes tier "copper" or "iron" (blocks/handle_placement), and the
# "liquid_pipe_netherite" item is crafted from iron ingots and named "L2 Iron
# Pipe" — the id is a leftover name, not a third tier. Left in place because a
# real netherite tier would need its own recipe, item and transfer values.

kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..0.9]
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
kill @s
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:copper_block","minecraft:item_name":'Copper Pipe',"minecraft:custom_data":{ra:{liquid_pipe_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pipe_copper"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
