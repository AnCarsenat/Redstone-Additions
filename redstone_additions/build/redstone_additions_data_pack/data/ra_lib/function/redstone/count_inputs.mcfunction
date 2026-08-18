# /ra_lib:redstone/count_inputs
# Count how many sides carry a redstone component, powered or not.
# As the marker, at the block. Output: @s ra.rs_inputs (0-6).
#
# Used by the AND and NAND gates, which have to know how many inputs exist before
# they can say whether all of them are on.
#
# This used to be a hand-copied mirror of the old detect: every source test
# written out again for all six directions, with a comment warning that the two
# had to be kept in step by hand. They never quite were — detect grew the
# ra.redstone.ignore_blocks opt-out and this file grew it too, but neither ever
# learned about pressure plates. Both now share ra_lib:redstone/has_input, so a
# source added to the block tags is counted here without anyone remembering to.

scoreboard players set @s ra.rs_inputs 0

execute store result score #rs.in ra.temp run function ra_lib:redstone/has_input {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side"}
scoreboard players operation @s ra.rs_inputs += #rs.in ra.temp

execute store result score #rs.in ra.temp run function ra_lib:redstone/has_input {dx:0,dy:0,dz:1,side:"south",back:"north",torch:"side"}
scoreboard players operation @s ra.rs_inputs += #rs.in ra.temp

execute store result score #rs.in ra.temp run function ra_lib:redstone/has_input {dx:-1,dy:0,dz:0,side:"west",back:"east",torch:"side"}
scoreboard players operation @s ra.rs_inputs += #rs.in ra.temp

execute store result score #rs.in ra.temp run function ra_lib:redstone/has_input {dx:1,dy:0,dz:0,side:"east",back:"west",torch:"side"}
scoreboard players operation @s ra.rs_inputs += #rs.in ra.temp

execute store result score #rs.in ra.temp run function ra_lib:redstone/has_input {dx:0,dy:-1,dz:0,side:"down",back:"up",torch:"below"}
scoreboard players operation @s ra.rs_inputs += #rs.in ra.temp

execute store result score #rs.in ra.temp run function ra_lib:redstone/has_input {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none"}
scoreboard players operation @s ra.rs_inputs += #rs.in ra.temp

return run scoreboard players get @s ra.rs_inputs
