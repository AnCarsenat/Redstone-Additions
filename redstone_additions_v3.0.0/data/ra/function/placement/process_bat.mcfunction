# /data/ra_lib/function/placement/process_bat.mcfunction
# Process a bat placement
# Context: as bat, at bat

tag @s add ra.processed

# Find nearest player (who spawned the bat)
tag @p add ra.placer

# Store bat position (already block-aligned)
execute store result score #x ra.temp run data get entity @s Pos[0]
execute store result score #y ra.temp run data get entity @s Pos[1]
execute store result score #z ra.temp run data get entity @s Pos[2]

# If a custom block marker already exists here, this is a duplicate placement attempt.
execute align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[tag=ra.custom_block,distance=..0.9,limit=1] run kill @s
execute align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[tag=ra.custom_block,distance=..0.9,limit=1] run return 0

# If block is occupied, stop
execute unless block ~ ~ ~ #air run kill @s
execute unless block ~ ~ ~ #air run return 0
    

# Get block definition from bat's custom data and route to registry
execute align xyz positioned ~0.5 ~0.5 ~0.5 run function #ra:placement_handlers

# Clean up
tag @a[tag=ra.placer] remove ra.placer
kill @s