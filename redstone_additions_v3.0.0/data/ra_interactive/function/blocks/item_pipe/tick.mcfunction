# /ra_interactive:blocks/item_pipe/tick
# Tick all item pipes. Called every tick.

# Check for break detection
execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run function ra_interactive:blocks/item_pipe/on_break
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] remove ra.broken
execute as @e[type=armor_stand,tag=ra.custom_block.item_pipe] at @s unless entity @e[type=marker,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1] unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=armor_stand,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s unless entity @e[type=marker,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1] run function ra_interactive:blocks/item_pipe/on_break
tag @e[type=armor_stand,tag=ra.broken,tag=ra.custom_block.item_pipe] remove ra.broken

# Clear per-tick chain traversal tags.
tag @e[type=marker,tag=ra.custom_block.item_pipe,tag=ra.pipe_chain_visited] remove ra.pipe_chain_visited
tag @e[type=armor_stand,tag=ra.custom_block.item_pipe,tag=ra.pipe_chain_visited] remove ra.pipe_chain_visited

# Process each item pipe
execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s run function ra_interactive:blocks/item_pipe/process
execute as @e[type=armor_stand,tag=ra.custom_block.item_pipe] at @s unless entity @e[type=marker,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1] run function ra_interactive:blocks/item_pipe/process

