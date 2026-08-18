# /ra_lib:redstone/torch/side {dx:0,dy:0,dz:-1,side:"north"}
# Internal: redstone torches on a horizontal neighbour.
#
# A standing torch is deliberately not read here. A redstone torch strongly powers
# the block ABOVE it and nothing else — it does not power the blocks beside it, so
# a torch standing next to this one is not an input.
#
# A wall torch does power the block beside it, with one exception: never the block
# it is mounted on. A wall torch points away from whatever it is attached to, so
# the one attached to US is the one facing $(side) — the direction from us towards
# that neighbour. Excluding that single facing is cheaper and clearer than listing
# the three that do count.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wall_torch[lit=true] unless block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wall_torch[facing=$(side)] run scoreboard players set #rs ra.temp 15
