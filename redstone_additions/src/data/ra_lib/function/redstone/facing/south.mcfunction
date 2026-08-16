# /ra_lib:redstone/facing/south
# Internal: map world-space power scores onto look-space ones for a marker
# facing south (ra.facing 3).

scoreboard players operation @s ra.power.front = @s ra.power.south
scoreboard players operation @s ra.power.back = @s ra.power.north
scoreboard players operation @s ra.power.left = @s ra.power.east
scoreboard players operation @s ra.power.right = @s ra.power.west
scoreboard players operation @s ra.power.local_up = @s ra.power.up
scoreboard players operation @s ra.power.local_down = @s ra.power.down
