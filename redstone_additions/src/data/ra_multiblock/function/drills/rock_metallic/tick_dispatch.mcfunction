# /ra_multiblock:drill/tick_dispatch
# Select all drill entities and run their tick
# Hook: #ra_lib_multiblock:tick

execute as @e[tag=ra.multiblock.drill] at @s run function ra_multiblock:drills/rock_metallic/tick
