# /ra_wires:electric/break/switch
# Clean up a broken switch and return its item.
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
# The switch is placed as waxed_cut_copper now, so that is what breaking it drops.
# The item model stays a redstone lamp so it is still tellable apart from a Valve
# in an inventory -- the pack already does this for wires, whose item is a candle.
kill @e[type=item,nbt={Item:{id:"minecraft:waxed_cut_copper"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:redstone_lamp","minecraft:item_name":'EU Switch',"minecraft:custom_data":{ra:{electric_switch:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_switch"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
