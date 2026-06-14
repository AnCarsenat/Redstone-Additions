# /ra_multiblock:upgrade_platform/check_structure
# Periodic structure integrity check for upgrade platform
# Context: as multiblock marker, at base position
# Hook: #ra_lib_multiblock:check_structure
# Sets: @s ra.multiblock score to 1 (intact) or 0 (broken)

execute unless entity @s[tag=ra.multiblock.upgrade_platform] run return 0

execute store result score @s ra.multiblock run function ra_multiblock:upgrade_platform/check_blocks
