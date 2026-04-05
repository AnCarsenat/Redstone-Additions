# /ra_wires:electric/tick
# Tick electric wires, generator, consumer, and switch

# Break detection
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s unless block ~ ~ ~ mud_brick_wall unless block ~ ~ ~ polished_blackstone_wall run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s unless block ~ ~ ~ blast_furnace run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s unless block ~ ~ ~ observer run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.electric_switch] at @s unless block ~ ~ ~ redstone_lamp run tag @s add ra.broken

execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run function ra_wires:blocks/on_break/electric_wire
execute as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.broken] at @s run function ra_wires:blocks/on_break/electric_generator
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.broken] at @s run function ra_wires:blocks/on_break/electric_consumer
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.broken] at @s run function ra_wires:blocks/on_break/electric_switch

tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_wire] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_generator] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_consumer] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_switch] remove ra.broken

# Ensure every electric node has complete runtime data before processing.
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/init_node

# Generator production
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s run function ra_wires:electric/generator_tick

# Transfer through network
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/process_source

# Consumer draw
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s run function ra_wires:electric/consumer_tick

# Status refresh
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/update_status
