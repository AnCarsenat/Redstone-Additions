# /ra:tools/goggles/scan_blocks
# Find nearby custom blocks and dispatch billboard creation
# Context: as player with goggles, at player position
# Max scan distance: 16 blocks
# Uses per-block draw_display functions and inlined display rules.

# Interactive blocks
execute as @e[tag=ra.custom_block.block_breaker,distance=..16] at @s run function ra_interactive:goggles/draw_display_block_breaker
execute as @e[tag=ra.custom_block.block_placer,distance=..16] at @s run function ra_interactive:goggles/draw_display_block_placer
execute as @e[tag=ra.custom_block.breeder,distance=..16] at @s run function ra_interactive:goggles/draw_display_breeder
execute as @e[tag=ra.custom_block.pusher,distance=..16] at @s run function ra_interactive:goggles/draw_display_pusher
execute as @e[tag=ra.custom_block.spitter,distance=..16] at @s run function ra_interactive:goggles/draw_display_spitter
execute as @e[tag=ra.custom_block.item_pipe,distance=..16] at @s run function ra_interactive:goggles/draw_display_item_pipe
execute as @e[tag=ra.custom_block.item_mover,distance=..16] at @s run function ra_interactive:goggles/draw_display_item_mover
execute as @e[tag=ra.custom_block.infinite_water_cauldron,distance=..16] at @s run function ra_interactive:goggles/draw_display_inf_water
execute as @e[tag=ra.custom_block.infinite_lava_cauldron,distance=..16] at @s run function ra_interactive:goggles/draw_display_inf_lava
execute as @e[tag=ra.custom_block.infinite_snow_cauldron,distance=..16] at @s run function ra_interactive:goggles/draw_display_inf_snow
execute as @e[tag=ra.custom_block.message_block,distance=..16] at @s run function ra_interactive:goggles/draw_display_message

# Storage blocks
execute as @e[tag=ra.custom_block.boxer,distance=..16] at @s run function ra_storage:goggles/draw_display_boxer
execute as @e[tag=ra.custom_block.unboxer,distance=..16] at @s run function ra_storage:goggles/draw_display_unboxer

# Logic gates
execute as @e[tag=ra.custom_block.uni_gate,distance=..16] at @s run function ra_gates:goggles/draw_display_uni_gate
execute as @e[tag=ra.custom_block.clock,distance=..16] at @s run function ra_gates:goggles/draw_display_clock
execute as @e[tag=ra.custom_block.delayer,distance=..16] at @s run function ra_gates:goggles/draw_display_delayer
execute as @e[tag=ra.custom_block.extender,distance=..16] at @s run function ra_gates:goggles/draw_display_extender
execute as @e[tag=ra.custom_block.rand,distance=..16] at @s run function ra_gates:goggles/draw_display_rand
execute as @e[tag=ra.custom_block.shortener,distance=..16] at @s run function ra_gates:goggles/draw_display_shortener

# Sensors
execute as @e[tag=ra.custom_block.entity_detector,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Entity Detector",show_name:1b,show_status:1b}
execute as @e[tag=ra.custom_block.entity_detector,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[tag=ra.custom_block.tag_adder,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Tag Adder"}
execute as @e[tag=ra.custom_block.tag_adder,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[tag=ra.custom_block.tag_remover,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Tag Remover"}
execute as @e[tag=ra.custom_block.tag_remover,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

# Wireless redstone
execute as @e[tag=ra.custom_block.emitter,distance=..16] at @s run function ra_wireless:goggles/draw_display_emitter
execute as @e[tag=ra.custom_block.receiver,distance=..16] at @s run function ra_wireless:goggles/draw_display_receiver

# Wires
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Plastic Pipe",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.liquid_tank,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Liquid Tank",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_tank,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.liquid_pump,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Liquid Pump",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_pump,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.liquid_valve,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Liquid Valve",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.liquid_drain,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Liquid Drain",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.liquid_drain,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.gas_tank,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Gas Tank",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.gas_tank,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.gas_pump,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Gas Pump",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.gas_pump,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.gas_valve,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Gas Valve",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.gas_valve,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.electric_wire,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"Wire",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.electric_wire,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.electric_generator,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"EU Generator",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.electric_generator,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.electric_consumer,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"EU Consumer",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute as @e[type=marker,tag=ra.custom_block.electric_switch,distance=..16] at @s run data modify storage ra:temp billboard set value {name:"EU Switch",show_name:1b,show_status:1b}
execute as @e[type=marker,tag=ra.custom_block.electric_switch,distance=..16] at @s run function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
