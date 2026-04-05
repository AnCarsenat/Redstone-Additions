# /ra_wires:blocks/on_break/electric_switch

tag @s remove ra.wires.node
tag @s remove ra.wires.electric_node
function ra_wires:common/update_model_local_and_neighbors

kill @e[type=item,nbt={Item:{id:"minecraft:redstone_lamp"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:redstone_lamp","minecraft:item_name":'EU Switch',"minecraft:custom_data":{ra:{electric_switch:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_switch"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

kill @s
