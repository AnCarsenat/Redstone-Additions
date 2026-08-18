# /ra_wires:electric/break/industrial_light
# Clean up a broken Industrial Light and return its item.
# Context: as the marker, at its position. Ends by killing the marker.
#
# The beam is put out BEFORE the marker dies. The marker is the only thing that
# knows where its own light blocks are, and once it is gone nothing in the world
# can find them — an invisible block with no source is not something a player can
# reasonably clear up.

function ra_wires:blocks/industrial_light/cast {mode:"off"}

tag @s remove ra.wires.electric_node
tag @s remove ra.wires.node
function ra_lib:transport/net/leave
function ra_wires:common/update_model_local_and_neighbors
kill @e[type=item,nbt={Item:{id:"minecraft:sea_lantern"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:sea_lantern","minecraft:item_name":'Industrial Light',"minecraft:lore":[{text:"Redstone + EU: a 10 block beam of light",color:"gray",italic:false}],"minecraft:custom_data":{ra:{industrial_light:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.industrial_light"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
