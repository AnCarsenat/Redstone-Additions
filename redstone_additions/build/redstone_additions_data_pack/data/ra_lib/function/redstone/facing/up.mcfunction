# /ra_lib:redstone/facing/up
# Internal: map world-space power scores onto look-space ones for a marker
# facing up (ra.facing 1).

scoreboard players operation @s ra.power.front = @s ra.power.up
scoreboard players operation @s ra.power.back = @s ra.power.down
scoreboard players operation @s ra.power.left = @s ra.power.east
scoreboard players operation @s ra.power.right = @s ra.power.west
scoreboard players operation @s ra.power.local_up = @s ra.power.north
scoreboard players operation @s ra.power.local_down = @s ra.power.south
