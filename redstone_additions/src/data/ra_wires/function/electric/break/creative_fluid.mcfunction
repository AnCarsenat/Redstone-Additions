# /ra_wires:electric/break/creative_fluid
# Clean up a broken Creative Fluid Source. Context: as the marker, at its position.

tag @s remove ra.wires.electric_node
tag @s remove ra.wires.fluid_node
tag @s remove ra.wires.node
function ra_lib:transport/net/leave
function ra_wires:common/update_model_local_and_neighbors
# Both creative sources are beacons now, so the radius is tightened to the block
# itself -- at distance 2 this would happily eat the OTHER one's dropped beacon
# when the two are placed next to each other.
kill @e[type=item,nbt={Item:{id:"minecraft:beacon"}},distance=..1,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:beacon","minecraft:item_name":'Creative Fluid Source',"minecraft:rarity":"epic","minecraft:lore":[{text:"Creative: makes something from nothing",color:"light_purple",italic:false}],"minecraft:custom_data":{ra:{creative_fluid:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.creative_fluid"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
