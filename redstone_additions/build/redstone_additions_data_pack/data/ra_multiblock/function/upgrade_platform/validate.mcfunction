# /ra_multiblock:upgrade_platform/validate
# Assembly-time validation for upgrade platform
# Context: at multiblock base position
# Hook: #ra_lib_multiblock:validate
# Returns: sets #mb_result ra.multiblock to 1 if valid

# Only handle upgrade_platform type
execute unless data storage ra:multiblock {type:"upgrade_platform"} run return 0

# Check structure (symmetric — no direction variants needed)
execute store result score #mb_result ra.multiblock run function ra_multiblock:upgrade_platform/check_blocks

# If valid, store assembly data (no facing for symmetric structure)
execute if score #mb_result ra.multiblock matches 1 run data modify storage ra:multiblock facing set value "none"
