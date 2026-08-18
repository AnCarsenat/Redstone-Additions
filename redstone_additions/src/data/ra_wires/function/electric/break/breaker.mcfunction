# /ra_wires:electric/break/breaker
# Clean up a broken EU Breaker and return its item.
# Context: as the marker, at its position. Ends by killing the marker.
#
# A breaker is a bridge, not a node, so there is no network membership to release
# — but the two grids it was joining are about to become permanently separate,
# and their connectors need redrawing.

tag @s remove ra.wires.bridge
tag @s remove ra.wires.node
function ra_wires:common/update_model_local_and_neighbors
kill @e[type=item,nbt={Item:{id:"minecraft:waxed_copper_bulb"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_copper_bulb","minecraft:item_name":'EU Breaker',"minecraft:lore":[{text:"Powered: evens EU between the grids",color:"gray",italic:false},{text:"on either side. Never merges them.",color:"gray",italic:false}],"minecraft:custom_data":{ra:{electric_breaker:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_breaker"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
