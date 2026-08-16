# /ra_multiblock:drills/rock_metallic/tick_dispatch
# Select all Rock Metallic Drill markers and run their tick.
# Hook: #ra_lib_multiblock:tick
#
# The tag comes from ra_lib_multiblock:generic/setup_type, which derives it from
# the registered id: ra.multiblock.{id}.

execute as @e[type=marker,tag=ra.multiblock.rock_metallic_drill] at @s run function ra_multiblock:drills/rock_metallic/tick
