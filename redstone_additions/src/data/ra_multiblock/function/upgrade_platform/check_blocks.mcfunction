# /ra_multiblock:upgrade_platform/check_blocks
# Check if the 3x1x3 upgrade platform structure is valid
# Context: at multiblock base position (center of platform)
# Returns: 1 if all blocks valid, 0 if any invalid. NO side effects.
#
# Structure (top view):
#   S S S
#   S N S     (S = smooth_stone, N = netherite base [center])
#   S S S

execute unless block ~-1 ~ ~-1 minecraft:smooth_stone run return 0
execute unless block ~0 ~ ~-1 minecraft:smooth_stone run return 0
execute unless block ~1 ~ ~-1 minecraft:smooth_stone run return 0
execute unless block ~-1 ~ ~0 minecraft:smooth_stone run return 0
execute unless block ~1 ~ ~0 minecraft:smooth_stone run return 0
execute unless block ~-1 ~ ~1 minecraft:smooth_stone run return 0
execute unless block ~0 ~ ~1 minecraft:smooth_stone run return 0
execute unless block ~1 ~ ~1 minecraft:smooth_stone run return 0

return 1
