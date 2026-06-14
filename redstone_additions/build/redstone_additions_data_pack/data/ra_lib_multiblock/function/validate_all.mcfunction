# /ra_lib_multiblock:validate_all
# Validate all existing multiblocks (called periodically)
# Disassembles any that have broken structures

execute as @e[tag=ra.multiblock] at @s run function ra_lib_multiblock:validate_single
