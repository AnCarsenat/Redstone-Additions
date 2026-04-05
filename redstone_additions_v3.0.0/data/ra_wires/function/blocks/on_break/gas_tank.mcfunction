# /ra_wires:blocks/on_break/gas_tank

tag @s remove ra.wires.node
tag @s remove ra.wires.fluid_node
tag @s remove ra.wires.liquid_node
tag @s remove ra.wires.gas_node
function ra_wires:common/update_model_local_and_neighbors

kill @e[type=item,nbt={Item:{id:"minecraft:iron_block"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:iron_block","minecraft:item_name":'Gas Tank',"minecraft:custom_data":{ra:{gas_tank:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.gas_tank"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

kill @s
