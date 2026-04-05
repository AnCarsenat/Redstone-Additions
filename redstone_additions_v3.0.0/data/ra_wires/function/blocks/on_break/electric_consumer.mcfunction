# /ra_wires:blocks/on_break/electric_consumer

tag @s remove ra.wires.node
tag @s remove ra.wires.electric_node
function ra_wires:common/update_model_local_and_neighbors

kill @e[type=item,nbt={Item:{id:"minecraft:observer"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:observer","minecraft:item_name":'EU Consumer',"minecraft:custom_data":{ra:{electric_consumer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_consumer"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

kill @s
