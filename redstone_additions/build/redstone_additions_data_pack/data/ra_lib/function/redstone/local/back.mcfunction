# /ra_lib:redstone/local/back
# Power arriving on this block's back side. 0-16.
# Context: as the block's marker, at the block position.
# Returns the level, and leaves it in #rs ra.temp.
#
# Named the way the player sees the block rather than the way the compass does:
# a machine that cares about "the side it faces" should not have to know it ended
# up pointing west. The world direction is resolved from the marker's own
# ra.facing, so a block rotated by the wrench keeps meaning the same thing.
#
# ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.


execute if score @s ra.facing matches 0 run return run function ra_lib:redstone/side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none"}
execute if score @s ra.facing matches 1 run return run function ra_lib:redstone/side {dx:0,dy:-1,dz:0,side:"down",back:"up",torch:"below"}
execute if score @s ra.facing matches 2 run return run function ra_lib:redstone/side {dx:0,dy:0,dz:1,side:"south",back:"north",torch:"side"}
execute if score @s ra.facing matches 3 run return run function ra_lib:redstone/side {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side"}
execute if score @s ra.facing matches 4 run return run function ra_lib:redstone/side {dx:1,dy:0,dz:0,side:"east",back:"west",torch:"side"}
execute if score @s ra.facing matches 5 run return run function ra_lib:redstone/side {dx:-1,dy:0,dz:0,side:"west",back:"east",torch:"side"}

# No facing score at all -- treat the block as if it faced south, which is the
# default ra_lib:placement/place seeds when there was no placer to read.
execute unless score @s ra.facing matches 0..5 run return run function ra_lib:redstone/side {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side"}
return 0
