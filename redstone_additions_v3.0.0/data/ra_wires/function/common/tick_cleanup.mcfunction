# /ra_wires:common/tick_cleanup
# Clear transient per-tick transport tags

tag @e[type=marker,tag=ra.wires.did_move] remove ra.wires.did_move
tag @e[type=marker,tag=ra.wires.liq_src] remove ra.wires.liq_src
tag @e[type=marker,tag=ra.wires.eu_src] remove ra.wires.eu_src
tag @e[type=marker,tag=ra.wires.legacy_gas_pipe] remove ra.wires.legacy_gas_pipe
tag @e[type=marker,tag=ra.wires.pipe_present,tag=!ra.custom_block.liquid_pipe,tag=!ra.custom_block.gas_pipe] remove ra.wires.pipe_present
tag @e[type=marker,tag=ra.wires.pipe_display_v2,tag=!ra.custom_block.liquid_pipe,tag=!ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v2
tag @e[type=marker,tag=ra.wires.pipe_display_v3,tag=!ra.custom_block.liquid_pipe,tag=!ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v3
tag @e[type=marker,tag=ra.wires.pipe_display_v4,tag=!ra.custom_block.liquid_pipe,tag=!ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v4
tag @e[type=marker,tag=ra.wires.pipe_display_v5,tag=!ra.custom_block.liquid_pipe,tag=!ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v5
