# /ra_wires:liquid/tick
# Tick merged fluid pipes, tanks, pumps, valves, and drains

# Keep legacy tags participating in the merged fluid network.
tag @e[type=marker,tag=ra.wires.liquid_node] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.wires.gas_node] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.liquid_pipe] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.liquid_tank] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.liquid_pump] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.liquid_valve] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.liquid_drain] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.gas_tank] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.gas_pump] add ra.wires.fluid_node
tag @e[type=marker,tag=ra.custom_block.gas_valve] add ra.wires.fluid_node

# Normalize pipe IO endpoint capabilities and tags.
function ra_wires:liquid/bootstrap_pipe_io_caps

# Ensure every node has complete runtime data before any processing.
execute as @e[type=marker,tag=ra.wires.fluid_node] at @s run function ra_wires:liquid/init_node

# Migrate old pane-based pipes to conduit visuals.
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ orange_stained_glass_pane run setblock ~ ~ ~ conduit[waterlogged=false]
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ black_stained_glass_pane run setblock ~ ~ ~ conduit[waterlogged=false]
execute as @e[type=marker,tag=ra.custom_block.gas_pipe] at @s if block ~ ~ ~ light_gray_stained_glass_pane run setblock ~ ~ ~ conduit[waterlogged=false]
execute as @e[type=marker,tag=ra.custom_block.gas_pipe] at @s if block ~ ~ ~ gray_stained_glass_pane run setblock ~ ~ ~ conduit[waterlogged=false]

# Force all managed conduit pipes to remain non-waterlogged.
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]
execute as @e[type=marker,tag=ra.custom_block.gas_pipe] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]

# Promote legacy gas pipe markers into merged fluid pipe markers.
tag @e[type=marker,tag=ra.custom_block.gas_pipe] add ra.wires.legacy_gas_pipe
tag @e[type=marker,tag=ra.wires.legacy_gas_pipe] add ra.custom_block.liquid_pipe
tag @e[type=marker,tag=ra.wires.legacy_gas_pipe] remove ra.custom_block.gas_pipe
tag @e[type=marker,tag=ra.wires.legacy_gas_pipe] add ra.wires.fluid_node
execute as @e[type=marker,tag=ra.wires.legacy_gas_pipe] at @s run function ra_wires:common/update_model_local_and_neighbors
tag @e[type=marker,tag=ra.wires.legacy_gas_pipe] remove ra.wires.legacy_gas_pipe

# Refresh pipe displays only when a conduit appears or disappears at a pipe marker.
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_present] at @s if block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_present] at @s if block ~ ~ ~ conduit run tag @s add ra.wires.pipe_present
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=!ra.wires.pipe_present] at @s if block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=!ra.wires.pipe_present] at @s if block ~ ~ ~ conduit run tag @s add ra.wires.pipe_present

execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit run function ra_wires:common/update_pipe_displays
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit run tag @s remove ra.wires.pipe_present
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit run function ra_wires:common/update_pipe_displays
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit run tag @s remove ra.wires.pipe_present

# Self-heal missing visuals without forcing per-tick rebuilds.
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.pipe_display.center,distance=..0.6,limit=1] run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_pipe] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.pipe_display.center,distance=..0.6,limit=1] run function ra_wires:common/update_model_local_and_neighbors

# One-time refresh when display geometry changes; avoids perpetual flicker.
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_display_v6] at @s if block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_display_v6] at @s if block ~ ~ ~ conduit run tag @s add ra.wires.pipe_display_v6
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=!ra.wires.pipe_display_v6] at @s if block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=!ra.wires.pipe_display_v6] at @s if block ~ ~ ~ conduit run tag @s add ra.wires.pipe_display_v6

# Break detection
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s unless block ~ ~ ~ conduit run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_tank] at @s unless block ~ ~ ~ waxed_copper_block run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_pump] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_valve] at @s unless block ~ ~ ~ waxed_cut_copper run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] at @s unless block ~ ~ ~ dropper run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_pipe] at @s unless block ~ ~ ~ conduit run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_tank] at @s unless block ~ ~ ~ iron_block run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_pump] at @s unless block ~ ~ ~ smoker run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_valve] at @s unless block ~ ~ ~ smooth_basalt run tag @s add ra.broken

execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..1.5]
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:conduit"}},distance=..2,limit=2]
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run execute if data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blue_candle","minecraft:item_name":'L2 Plastic Pipe',"minecraft:custom_data":{ra:{liquid_pipe_netherite:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pipe_netherite"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run execute unless data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:light_blue_candle","minecraft:item_name":'L1 Plastic Pipe',"minecraft:custom_data":{ra:{liquid_pipe_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pipe_copper"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:waxed_copper_block"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_copper_block","minecraft:item_name":'Liquid Tank',"minecraft:custom_data":{ra:{liquid_tank:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_tank"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":'Liquid Pump',"minecraft:custom_data":{ra:{liquid_pump:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pump"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:waxed_cut_copper"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_cut_copper","minecraft:item_name":'Liquid Valve',"minecraft:custom_data":{ra:{liquid_valve:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_valve"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dropper"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dropper","minecraft:item_name":'Liquid Drain',"minecraft:custom_data":{ra:{liquid_drain:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_drain"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..1.5]
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:conduit"}},distance=..2,limit=2]
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:light_gray_stained_glass_pane"}},distance=..2,limit=2]
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:gray_stained_glass_pane"}},distance=..2,limit=2]
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run execute if data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blue_candle","minecraft:item_name":'L2 Plastic Pipe',"minecraft:custom_data":{ra:{liquid_pipe_netherite:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pipe_netherite"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run execute unless data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:light_blue_candle","minecraft:item_name":'L1 Plastic Pipe',"minecraft:custom_data":{ra:{liquid_pipe_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.liquid_pipe_copper"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:iron_block"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:iron_block","minecraft:item_name":'Gas Tank',"minecraft:custom_data":{ra:{gas_tank:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.gas_tank"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:smoker"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:smoker","minecraft:item_name":'Gas Pump',"minecraft:custom_data":{ra:{gas_pump:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.gas_pump"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run tag @s remove ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run tag @s remove ra.wires.liquid_node
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run tag @s remove ra.wires.gas_node
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:smooth_basalt"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:smooth_basalt","minecraft:item_name":'Gas Valve',"minecraft:custom_data":{ra:{gas_valve:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.gas_valve"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run kill @s

tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.liquid_pipe] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.liquid_tank] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.liquid_pump] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.liquid_valve] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.liquid_drain] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.gas_pipe] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.gas_tank] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.gas_pump] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.gas_valve] remove ra.broken

# Source/sink generation
execute as @e[type=marker,tag=ra.custom_block.liquid_pump] at @s run function ra_wires:liquid/pump_tick
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] at @s run function ra_wires:liquid/drain_tick
execute as @e[type=marker,tag=ra.custom_block.gas_pump] at @s run function ra_wires:gas/pump_tick

# Tanks pull first so adjacent filled pipes are consumed reliably.
execute as @e[type=marker,tag=ra.custom_block.liquid_tank] at @s run function ra_wires:liquid/tank_receive_prepass

# Transfer through network
execute as @e[type=marker,tag=ra.wires.pipe_io_endpoint] at @s run function ra_wires:liquid/process_source

# Status refresh
execute as @e[type=marker,tag=ra.wires.fluid_node] at @s run function ra_wires:liquid/update_status
