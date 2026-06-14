# /ra_wires:liquid/bootstrap_pipe_io_caps
# Normalize explicit pipe I/O capabilities and endpoint tags.

# Rebuild endpoint tags every tick to avoid stale capabilities.
tag @e[type=marker,tag=ra.wires.pipe_io_endpoint] remove ra.wires.pipe_io_endpoint

# Native fluid-network nodes are always pipe endpoints.
tag @e[type=marker,tag=ra.wires.fluid_node] add ra.wires.pipe_io_endpoint

# Any RA custom block that explicitly declares pipe I/O is also an endpoint.
execute as @e[type=marker,tag=ra.custom_block] if data entity @s data.properties.pipe_io run tag @s add ra.wires.pipe_io_endpoint

# Default capabilities for native fluid blocks.
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] unless data entity @s data.properties.pipe_io run data modify entity @s data.properties.pipe_io set value {input:1b,output:1b}
execute as @e[type=marker,tag=ra.custom_block.gas_pipe] unless data entity @s data.properties.pipe_io run data modify entity @s data.properties.pipe_io set value {input:1b,output:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_tank] unless data entity @s data.properties.pipe_io run data modify entity @s data.properties.pipe_io set value {input:1b,output:1b}
execute as @e[type=marker,tag=ra.custom_block.gas_tank] unless data entity @s data.properties.pipe_io run data modify entity @s data.properties.pipe_io set value {input:1b,output:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_valve] unless data entity @s data.properties.pipe_io run data modify entity @s data.properties.pipe_io set value {input:1b,output:1b}
execute as @e[type=marker,tag=ra.custom_block.gas_valve] unless data entity @s data.properties.pipe_io run data modify entity @s data.properties.pipe_io set value {input:1b,output:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] unless data entity @s data.properties.pipe_io run data modify entity @s data.properties.pipe_io set value {input:1b,output:1b}

# Pumps stay output-only.
execute as @e[type=marker,tag=ra.custom_block.liquid_pump] run data modify entity @s data.properties.pipe_io set value {input:0b,output:1b}
execute as @e[type=marker,tag=ra.custom_block.gas_pump] run data modify entity @s data.properties.pipe_io set value {input:0b,output:1b}

# Ensure endpoints keep complete flag shape.
execute as @e[type=marker,tag=ra.wires.pipe_io_endpoint] if data entity @s data.properties.pipe_io unless data entity @s data.properties.pipe_io.input run data modify entity @s data.properties.pipe_io.input set value 1b
execute as @e[type=marker,tag=ra.wires.pipe_io_endpoint] if data entity @s data.properties.pipe_io unless data entity @s data.properties.pipe_io.output run data modify entity @s data.properties.pipe_io.output set value 1b