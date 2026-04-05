# /ra_wires:goggles/draw_displays
# Draw goggles billboards for RA Wires blocks
# Context: as player with goggles, at player position

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
