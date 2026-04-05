# /ra_interactive:blocks/item_pipe/tick
# Tick all item pipes. Called every tick.

# Keep marker anchors snapped to centered block coordinates.
execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~

# Break detection
execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run function ra_interactive:blocks/item_pipe/on_break
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] remove ra.broken

# Clear per-tick chain traversal tags.
tag @e[type=marker,tag=ra.custom_block.item_pipe,tag=ra.pipe_chain_visited] remove ra.pipe_chain_visited

# Process each item pipe
execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s run function ra_interactive:blocks/item_pipe/process

