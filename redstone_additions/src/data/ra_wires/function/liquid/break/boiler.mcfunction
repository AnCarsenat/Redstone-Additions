# /ra_wires:liquid/break/boiler
# Clean up a broken Boiler and return its item.
# Context: as the marker, at its position. Ends by killing the marker.
#
# The boiler is not a network node, so there is no network membership to release.

kill @e[type=item,nbt={Item:{id:"minecraft:furnace"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:furnace","minecraft:item_name":'Boiler',"minecraft:custom_data":{ra:{boiler:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.boiler"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
