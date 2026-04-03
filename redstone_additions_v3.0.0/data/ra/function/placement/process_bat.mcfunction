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
execute if entity @e[type=armor_stand,tag=ra.custom_block,distance=..0.25,limit=1] run kill @s
execute if entity @e[type=armor_stand,tag=ra.custom_block,distance=..0.25,limit=1] run return 0

# Get block definition from bat's custom data and route to registry
function #ra:placement_handlers

# Clean up
tag @a[tag=ra.placer] remove ra.placer
kill @s