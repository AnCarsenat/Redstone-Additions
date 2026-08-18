# /ra_lib:redstone/detect
# Read the redstone power reaching this block. As the marker, at the block.
#
# Output:
#   ra.power                              strongest side, 0-16
#   ra.power.north/south/east/west/up/down  per side, 0-16
#   ra.powered                            set when ra.power is 1 or more
#   ra.powered.strong                     set when ra.power is 16
#
# Returns ra.power, so a caller can branch on it without reading the score back.
#
# WHAT CHANGED, AND WHY IT COSTS LESS
# This used to be seven hand-written source scanners, each repeating the same six
# directions with the offsets typed out by hand — 505 lines whose dust section
# alone was fifteen near-identical comparisons times four. Every side is now one
# call to ra_lib:redstone/side, which knows every source once. Adding a source is
# a block-tag edit; it can no longer be added to one direction and forgotten in
# another, which is how pressure plates came to be missing from all six.
#
# The rest of the saving is in what is no longer computed by default. The
# look-space scores and the twelve direction tags had, at the time of writing,
# exactly zero readers anywhere in the pack, yet every call cleared twenty-two
# tags and wrote six extra scores to keep them current. They now live in
# ra_lib:redstone/detect_local, which costs nothing unless a block asks for it.
#
# The per-source tags (ra.powered.dust, .lever, .torch and the rest) are gone.
# Nothing read them either, and reproducing them would mean the per-side reader
# reporting which source won rather than just how much — a cost paid on every
# call for information nobody wanted.
#
# CHEAPER STILL
# A block that only needs "am I on?" should call ra_lib:redstone/any, and one that
# cares about a single face should call ra_lib:redstone/local/front and friends.
# A block whose vanilla base already carries the answer — dispenser `triggered`,
# note block `powered`, redstone lamp `lit` — should read the block state and call
# nothing at all.

execute store result score @s ra.power.north run function ra_lib:redstone/side {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side"}
execute store result score @s ra.power.south run function ra_lib:redstone/side {dx:0,dy:0,dz:1,side:"south",back:"north",torch:"side"}
execute store result score @s ra.power.west run function ra_lib:redstone/side {dx:-1,dy:0,dz:0,side:"west",back:"east",torch:"side"}
execute store result score @s ra.power.east run function ra_lib:redstone/side {dx:1,dy:0,dz:0,side:"east",back:"west",torch:"side"}
execute store result score @s ra.power.down run function ra_lib:redstone/side {dx:0,dy:-1,dz:0,side:"down",back:"up",torch:"below"}
execute store result score @s ra.power.up run function ra_lib:redstone/side {dx:0,dy:1,dz:0,side:"up",back:"down",torch:"none"}

# Aggregate: the strongest side wins.
scoreboard players operation @s ra.power = @s ra.power.north
execute if score @s ra.power.south > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.south
execute if score @s ra.power.west > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.west
execute if score @s ra.power.east > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.east
execute if score @s ra.power.down > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.down
execute if score @s ra.power.up > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.up

# Both tags are removed unconditionally first: a block that was powered last tick
# and is not now has to lose them, and these two are the only ones this function
# is responsible for.
tag @s remove ra.powered
tag @s remove ra.powered.strong
execute if score @s ra.power matches 1.. run tag @s add ra.powered
execute if score @s ra.power matches 16 run tag @s add ra.powered.strong

return run scoreboard players get @s ra.power
