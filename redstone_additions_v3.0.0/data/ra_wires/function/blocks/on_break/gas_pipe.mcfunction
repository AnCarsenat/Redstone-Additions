# /ra_wires:blocks/on_break/gas_pipe
# Break handler for legacy gas pipe variants

tag @s remove ra.wires.node
tag @s remove ra.wires.fluid_node
tag @s remove ra.wires.liquid_node
tag @s remove ra.wires.gas_node
kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..1.5]
function ra_wires:common/update_model_local_and_neighbors

kill @e[type=item,nbt={Item:{id:"minecraft:conduit"}},distance=..2,limit=2]
kill @e[type=item,nbt={Item:{id:"minecraft:light_gray_stained_glass_pane"}},distance=..2,limit=2]
kill @e[type=item,nbt={Item:{id:"minecraft:gray_stained_glass_pane"}},distance=..2,limit=2]

execute if data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blue_candle","minecraft:item_name":'L2 Plastic Pipe',"minecraft:custom_data":{ra:{liquid_pipe_netherite:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pipe_netherite"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute unless data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:light_blue_candle","minecraft:item_name":'L1 Plastic Pipe',"minecraft:custom_data":{ra:{liquid_pipe_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pipe_copper"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

kill @s
