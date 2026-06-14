# /ra_multiblock:blast_forge/tick_dispatch
# Select all blast forge entities and run their tick
# Hook: #ra_lib_multiblock:tick

execute as @e[tag=ra.multiblock.blast_forge] at @s run function ra_multiblock:blast_forge/tick
