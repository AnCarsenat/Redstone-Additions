# /ra_lib:redstone/facing/down
# Internal: map world-space power scores onto look-space ones for a marker
# facing down (ra.facing 0).
#
# The mirror of ra_lib:redstone/facing/up — see that file for why left and right
# on a vertically placed block are a convention rather than a derivation, and why
# these two must disagree about which way is left.

scoreboard players operation @s ra.power.front = @s ra.power.down
scoreboard players operation @s ra.power.back = @s ra.power.up
scoreboard players operation @s ra.power.left = @s ra.power.west
scoreboard players operation @s ra.power.right = @s ra.power.east
scoreboard players operation @s ra.power.local_up = @s ra.power.north
scoreboard players operation @s ra.power.local_down = @s ra.power.south
