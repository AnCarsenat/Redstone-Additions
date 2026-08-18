# /ra_lib:redstone/facing/up
# Internal: map world-space power scores onto look-space ones for a marker
# facing up (ra.facing 1).
#
# A block placed pointing straight up or down stores no yaw, so left, right,
# local_up and local_down cannot be derived — they are a convention. The one
# chosen here is that up and down are mirror images of each other, which is what
# rotating the block through the horizontal would do: facing up has left=east,
# facing down has left=west. They used to agree instead, so flipping a block over
# left its left and right hands unchanged, which no real rotation does.
#
# Front and back are the only two that are facts on a vertical block.

scoreboard players operation @s ra.power.front = @s ra.power.up
scoreboard players operation @s ra.power.back = @s ra.power.down
scoreboard players operation @s ra.power.left = @s ra.power.east
scoreboard players operation @s ra.power.right = @s ra.power.west
scoreboard players operation @s ra.power.local_up = @s ra.power.north
scoreboard players operation @s ra.power.local_down = @s ra.power.south
