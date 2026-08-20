# /ra_interactive:blocks/item_pipe/tick
# Tick all item pipes. Called every tick.

# Keep marker anchors snapped to centered block coordinates.
execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~

# Break detection
execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run execute if data block ~ ~ ~ Items[0] run summon item ~ ~0.5 ~ {Tags:["ra","ra.drop_temp"]}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[0]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":'Item Pipe',"minecraft:custom_data":{ra:{item_pipe:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.item_pipe"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run function ra_lib:transport/net/leave
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] remove ra.broken

# The filter display goes with the pipe.
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.item_pipe] at @s run kill @e[type=item_display,tag=ra.pipe_filter,distance=..0.6]

# Clear per-tick chain traversal tags.
tag @e[type=marker,tag=ra.custom_block.item_pipe,tag=ra.pipe_chain_visited] remove ra.pipe_chain_visited

# Process each item pipe
# Filter displays are upkeep, not logic -- every 20 ticks is plenty, and it keeps
# a wall of sorting pipes off the per-tick budget. ra.filter_cd used to pace the
# item frame rescan and now paces this instead.
scoreboard players add #pipe_disp ra.filter_cd 1
execute if score #pipe_disp ra.filter_cd matches 20.. run scoreboard players set #pipe_disp ra.filter_cd 0
execute if score #pipe_disp ra.filter_cd matches 0 as @e[type=marker,tag=ra.custom_block.item_pipe] at @s run function ra_interactive:blocks/item_pipe/refresh_display

execute as @e[type=marker,tag=ra.custom_block.item_pipe] at @s run function ra_interactive:blocks/item_pipe/process

