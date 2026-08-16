# /ra_multiblock:upgrade_platform/tick_dispatch
# Select all upgrade platform entities and run their tick
# Hook: #ra_lib_multiblock:tick

execute as @e[type=marker,tag=ra.multiblock.upgrade_platform] at @s run function ra_multiblock:upgrade_platform/tick
