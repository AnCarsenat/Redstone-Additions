# /ra_lib:redstone/any
# Is this block powered from any side at all? Returns 1 or 0.
# Context: as the block's marker, at the block position.
#
# The cheap question, for the many blocks that only ever ask "am I on?". It reads
# no levels, writes no scores and sets no tags, and it stops at the first side
# that answers yes.
#
# BEFORE YOU CALL THIS
# If the block underneath is one vanilla already tracks, do not call this at all —
# read the block state and pay nothing. A dispenser or dropper carries `triggered`,
# a note block and a door carry `powered`, a redstone lamp carries `lit`. The
# Block Breaker already does exactly this with `dispenser[triggered=true]`. This
# function is for the blocks with no such state — quartz, purpur, bookshelf,
# terracotta — where the neighbours have to be asked directly.

# `execute if function` cannot carry macro arguments, so each side's answer is
# captured with `store result` and tested straight after. Still early-exits: the
# `return 1` leaves this function before the remaining sides are asked.
# #rs.any is a separate holder from the #rs that any_side works in, so reading the
# answer cannot disturb how it was reached.

execute store result score #rs.any ra.temp run function ra_lib:redstone/any_side {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side",dust:"side"}
execute if score #rs.any ra.temp matches 1.. run return 1

execute store result score #rs.any ra.temp run function ra_lib:redstone/any_side {dx:0,dy:0,dz:1,side:"south",back:"north",torch:"side",dust:"side"}
execute if score #rs.any ra.temp matches 1.. run return 1

execute store result score #rs.any ra.temp run function ra_lib:redstone/any_side {dx:-1,dy:0,dz:0,side:"west",back:"east",torch:"side",dust:"side"}
execute if score #rs.any ra.temp matches 1.. run return 1

execute store result score #rs.any ra.temp run function ra_lib:redstone/any_side {dx:1,dy:0,dz:0,side:"east",back:"west",torch:"side",dust:"side"}
execute if score #rs.any ra.temp matches 1.. run return 1

execute store result score #rs.any ra.temp run function ra_lib:redstone/any_side {dx:0,dy:-1,dz:0,side:"down",back:"up",torch:"below",dust:"none"}
execute if score #rs.any ra.temp matches 1.. run return 1

execute store result score #rs.any ra.temp run function ra_lib:redstone/any_side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none",dust:"above"}
execute if score #rs.any ra.temp matches 1.. run return 1

return 0
