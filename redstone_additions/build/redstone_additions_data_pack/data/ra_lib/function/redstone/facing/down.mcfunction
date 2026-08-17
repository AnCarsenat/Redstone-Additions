# /ra_lib:redstone/facing/down
# Internal: map world-space power scores onto look-space ones for a marker
# facing down (ra.facing 0).

scoreboard players operation @s ra.power.front = @s ra.power.down
scoreboard players operation @s ra.power.back = @s ra.power.up
scoreboard players operation @s ra.power.left = @s ra.power.east
scoreboard players operation @s ra.power.right = @s ra.power.west
scoreboard players operation @s ra.power.local_up = @s ra.power.south
scoreboard players operation @s ra.power.local_down = @s ra.power.north
