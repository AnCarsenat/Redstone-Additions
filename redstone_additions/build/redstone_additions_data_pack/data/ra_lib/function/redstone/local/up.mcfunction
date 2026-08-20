# /ra_lib:redstone/local/up
# Power arriving on this block's local up side. 0-16.
# Context: as the block's marker, at the block position.
# Returns the level, and leaves it in #rs ra.temp.
#
# Named the way the player sees the block rather than the way the compass does:
# a machine that cares about "the side it faces" should not have to know it ended
# up pointing west. The world direction is resolved from the marker's own
# ra.facing, so a block rotated by the wrench keeps meaning the same thing.
#
# ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
#
# A block facing straight up or down has no yaw stored, so left, right, up and
# down on those two facings are a convention rather than a fact -- they keep the
# mapping the old look-space scores used, so nothing that relied on it changes.
# Only front and back are unambiguous for a vertically placed block.


execute if score @s ra.facing matches 0 run return run function ra_lib:redstone/side {dx:0,dy:0,dz:1,side:"south",back:"north",torch:"side",dust:"side"}
execute if score @s ra.facing matches 1 run return run function ra_lib:redstone/side {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side",dust:"side"}
execute if score @s ra.facing matches 2 run return run function ra_lib:redstone/side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none",dust:"above"}
execute if score @s ra.facing matches 3 run return run function ra_lib:redstone/side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none",dust:"above"}
execute if score @s ra.facing matches 4 run return run function ra_lib:redstone/side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none",dust:"above"}
execute if score @s ra.facing matches 5 run return run function ra_lib:redstone/side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none",dust:"above"}

# No facing score at all -- treat the block as if it faced south, which is the
# default ra_lib:placement/place seeds when there was no placer to read.
execute unless score @s ra.facing matches 0..5 run return run function ra_lib:redstone/side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none",dust:"above"}
return 0
