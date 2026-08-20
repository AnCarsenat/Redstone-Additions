# /ra_wires:liquid/break/filter
# Clean up a broken Liquid Filter and return its item.
# Context: as the marker, at its position. Ends by killing the marker.

tag @s remove ra.wires.fluid_node
tag @s remove ra.wires.electric_node
tag @s remove ra.wires.node
function ra_lib:transport/net/leave
function ra_wires:common/update_model_local_and_neighbors
kill @e[type=item,nbt={Item:{id:"minecraft:waxed_exposed_cut_copper"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_exposed_cut_copper","minecraft:item_name":'Liquid Filter',"minecraft:lore":[{text:"Passes only the medium it is set to",color:"gray",italic:false},{text:"Set filter_medium with the Data Handler",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{liquid_filter:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.liquid_filter"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
