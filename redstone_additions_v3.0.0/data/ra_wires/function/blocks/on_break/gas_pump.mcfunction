# /ra_wires:blocks/on_break/gas_pump

tag @s remove ra.wires.node
tag @s remove ra.wires.fluid_node
tag @s remove ra.wires.liquid_node
tag @s remove ra.wires.gas_node
function ra_wires:common/update_model_local_and_neighbors

kill @e[type=item,nbt={Item:{id:"minecraft:smoker"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:smoker","minecraft:item_name":'Gas Pump',"minecraft:custom_data":{ra:{gas_pump:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.gas_pump"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

kill @s
