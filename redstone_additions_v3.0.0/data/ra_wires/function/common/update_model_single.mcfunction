# /ra_wires:common/update_model_single
# Update connection-derived status for a single node

function ra_lib:transport/update_connection_status
execute if entity @s[tag=ra.custom_block.liquid_pipe] run function ra_wires:common/update_pipe_displays
execute if entity @s[tag=ra.custom_block.gas_pipe] run function ra_wires:common/update_pipe_displays
