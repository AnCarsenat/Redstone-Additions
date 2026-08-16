# /ra_lib:redstone/tags
# Internal: apply the ra.powered.* direction tags.
# Only reached when ra.power is non-zero. ra.power is the maximum over the six
# world directions and the look-space scores are copies of those, so a zero
# aggregate guarantees every individual direction is zero too and none of these
# tags could apply.

tag @s add ra.powered
execute if score @s ra.power matches 16 run tag @s add ra.powered.strong

execute if score @s ra.power.north matches 1.. run tag @s add ra.powered.north
execute if score @s ra.power.south matches 1.. run tag @s add ra.powered.south
execute if score @s ra.power.east matches 1.. run tag @s add ra.powered.east
execute if score @s ra.power.west matches 1.. run tag @s add ra.powered.west
execute if score @s ra.power.up matches 1.. run tag @s add ra.powered.up
execute if score @s ra.power.down matches 1.. run tag @s add ra.powered.down
execute if score @s ra.power.front matches 1.. run tag @s add ra.powered.front
execute if score @s ra.power.back matches 1.. run tag @s add ra.powered.back
execute if score @s ra.power.left matches 1.. run tag @s add ra.powered.left
execute if score @s ra.power.right matches 1.. run tag @s add ra.powered.right
execute if score @s ra.power.local_up matches 1.. run tag @s add ra.powered.local_up
execute if score @s ra.power.local_down matches 1.. run tag @s add ra.powered.local_down
