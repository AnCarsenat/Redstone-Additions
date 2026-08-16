# /ra_wires:electric/tick
# Tick electric wires, generator, consumer, and switch
#
# Electric still moves charge node to node; only the fluid side moved onto the
# shared transport network engine.

# Keep electric wire conduits non-waterlogged.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]

# Self-heal missing wire displays without forcing per-tick full rebuilds.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.wire_display.center,distance=..0.6,limit=1] run function ra_wires:common/update_model_local_and_neighbors

# Break detection — straight to the cleanup handler, no ra.broken round-trip.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s unless block ~ ~ ~ conduit run function ra_wires:electric/break/wire
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s unless block ~ ~ ~ blast_furnace run function ra_wires:electric/break/generator
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s unless block ~ ~ ~ observer run function ra_wires:electric/break/consumer
execute as @e[type=marker,tag=ra.custom_block.electric_switch] at @s unless block ~ ~ ~ redstone_lamp run function ra_wires:electric/break/switch
execute as @e[type=marker,tag=ra.custom_block.solar_panel] at @s unless block ~ ~ ~ daylight_detector run function ra_wires:electric/break/solar_panel

# Ensure every electric node has complete runtime data before processing.
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/init_node

# Generator production
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s run function ra_wires:electric/generator_tick
execute as @e[type=marker,tag=ra.custom_block.solar_panel] at @s run function ra_wires:blocks/solar_panel/tick

# Transfer through network
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/process_source

# Consumer draw
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s run function ra_wires:electric/consumer_tick

# Status refresh
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/update_status
