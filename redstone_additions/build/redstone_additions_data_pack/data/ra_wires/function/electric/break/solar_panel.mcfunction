# /ra_wires:electric/break/solar_panel
# Clean up a broken Solar Panel and return its item.
# Context: as the marker, at its position. Ends by killing the marker.

tag @s remove ra.wires.electric_node
tag @s remove ra.wires.node
kill @e[type=item,nbt={Item:{id:"minecraft:daylight_detector"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:daylight_detector","minecraft:item_name":'Solar Panel',"minecraft:custom_data":{ra:{solar_panel:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.solar_panel"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
