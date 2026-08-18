# /ra_wires:electric/break/battery
# Clean up a broken Battery and return its item.
# Context: as the marker, at its position. Ends by killing the marker.
#
# Breaking a battery takes 10000 EU of capacity out of the grid with it. The
# rebuild's clamp step spills whatever the shrunken grid can no longer hold,
# which is the honest outcome — the charge was in the battery.

tag @s remove ra.wires.electric_node
tag @s remove ra.wires.node
function ra_lib:transport/net/leave
function ra_wires:common/update_model_local_and_neighbors
kill @e[type=item,nbt={Item:{id:"minecraft:waxed_copper_grate"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_copper_grate","minecraft:item_name":'Battery',"minecraft:lore":[{text:"Stores 10000 EU for its grid",color:"gray",italic:false}],"minecraft:custom_data":{ra:{battery:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.battery"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
