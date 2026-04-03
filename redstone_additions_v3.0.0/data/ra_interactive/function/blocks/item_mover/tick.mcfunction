# /ra_interactive:blocks/item_mover/tick
# Tick all item movers

# Break detection — dispenser removed
execute as @e[tag=ra.custom_block.item_mover] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.item_mover] at @s run function ra_interactive:blocks/item_mover/on_break
tag @e[tag=ra.broken,tag=ra.custom_block.item_mover] remove ra.broken

# Process each item mover
execute as @e[tag=ra.custom_block.item_mover] at @s run function ra_interactive:blocks/item_mover/process
