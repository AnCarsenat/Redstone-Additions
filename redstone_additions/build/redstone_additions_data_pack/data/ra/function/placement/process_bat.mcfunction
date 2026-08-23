# /data/ra_lib/function/placement/process_bat.mcfunction
# Process a bat placement
# Context: as bat, at bat

tag @s add ra.processed

# Find nearest player (who spawned the bat)
tag @p add ra.placer

# If a custom block marker already exists here, this is a duplicate placement attempt.
execute align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[type=marker,tag=ra.custom_block,distance=..0.9,limit=1] run kill @s
execute align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[type=marker,tag=ra.custom_block,distance=..0.9,limit=1] run return 0

# If the space is occupied, stop.
#
# REPLACEABLE, NOT AIR
# This asked for #air, which is air, cave air and void air and nothing else. That
# is not where a spawn egg puts its mob. Vanilla spawns at the clicked block's own
# position when that block has an EMPTY COLLISION SHAPE, and only at the adjacent
# position otherwise -- so clicking tall grass, a flower, a rail, a pressure plate
# or a carpet put the bat inside that block rather than beside it, the test found
# something that was not air, and the placement was refused. Every custom block in
# the pack was unplaceable onto anything soft, which is most of a grassy field.
#
# #minecraft:replaceable is the tag that means exactly "a placed block may take
# this space" -- the same set vanilla itself overwrites when you place a stone on
# grass -- so it is the right question, and set_block's `replace` mode then does
# what the name says.
execute unless block ~ ~ ~ #minecraft:replaceable run kill @s
execute unless block ~ ~ ~ #minecraft:replaceable run return 0


# Is this block type switched off in the settings? Checked before the handlers
# run, so a disabled block never gets as far as creating a marker.
function ra_settings:placement/check
execute if score #blocked ra.set.tmp matches 1 run function ra_settings:placement/refuse
# The placer tag is cleaned up here as well as at the end, because this path
# returns before ever reaching the end and a stale ra.placer would attach the
# next placement to the wrong person.
execute if score #blocked ra.set.tmp matches 1 run tag @a[tag=ra.placer] remove ra.placer
execute if score #blocked ra.set.tmp matches 1 run kill @s
execute if score #blocked ra.set.tmp matches 1 run return 0

# Get block definition from bat's custom data and route to registry
execute align xyz positioned ~0.5 ~0.5 ~0.5 run function #ra:placement_handlers

# Configured property defaults, applied to the marker the handler just made.
execute align xyz positioned ~0.5 ~0.5 ~0.5 as @e[type=marker,tag=ra.custom_block,distance=..0.9,limit=1] run function ra_settings:placement/seed

# Clean up
tag @a[tag=ra.placer] remove ra.placer
kill @s
