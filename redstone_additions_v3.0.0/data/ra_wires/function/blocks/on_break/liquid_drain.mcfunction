# /ra_wires:blocks/on_break/liquid_drain

tag @s remove ra.wires.node
tag @s remove ra.wires.fluid_node
tag @s remove ra.wires.liquid_node
tag @s remove ra.wires.gas_node
function ra_wires:common/update_model_local_and_neighbors

kill @e[type=item,nbt={Item:{id:"minecraft:dropper"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dropper","minecraft:item_name":'Liquid Drain',"minecraft:custom_data":{ra:{liquid_drain:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_drain"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

kill @s
