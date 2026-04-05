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
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_display_v5] at @s if block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_display_v5] at @s if block ~ ~ ~ conduit run tag @s add ra.wires.pipe_display_v5
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=!ra.wires.pipe_display_v5] at @s if block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=!ra.wires.pipe_display_v5] at @s if block ~ ~ ~ conduit run tag @s add ra.wires.pipe_display_v5

# Break detection
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s unless block ~ ~ ~ conduit run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_tank] at @s unless block ~ ~ ~ copper_block run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_pump] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_valve] at @s unless block ~ ~ ~ cut_copper run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] at @s unless block ~ ~ ~ dropper run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_pipe] at @s unless block ~ ~ ~ conduit run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_tank] at @s unless block ~ ~ ~ iron_block run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_pump] at @s unless block ~ ~ ~ smoker run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.gas_valve] at @s unless block ~ ~ ~ smooth_basalt run tag @s add ra.broken

execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.broken] at @s run function ra_wires:blocks/on_break/liquid_pipe
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.broken] at @s run function ra_wires:blocks/on_break/liquid_tank
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.broken] at @s run function ra_wires:blocks/on_break/liquid_pump
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.broken] at @s run function ra_wires:blocks/on_break/liquid_valve
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.broken] at @s run function ra_wires:blocks/on_break/liquid_drain
execute as @e[type=marker,tag=ra.custom_block.gas_pipe,tag=ra.broken] at @s run function ra_wires:blocks/on_break/gas_pipe
execute as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.broken] at @s run function ra_wires:blocks/on_break/gas_tank
execute as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.broken] at @s run function ra_wires:blocks/on_break/gas_pump
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.broken] at @s run function ra_wires:blocks/on_break/gas_valve

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

# Transfer through network
execute as @e[type=marker,tag=ra.wires.fluid_node] at @s run function ra_wires:liquid/process_source

# Status refresh
execute as @e[type=marker,tag=ra.wires.fluid_node] at @s run function ra_wires:liquid/update_status
