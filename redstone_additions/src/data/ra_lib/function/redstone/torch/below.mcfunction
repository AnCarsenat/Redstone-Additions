# /ra_lib:redstone/torch/below {dx:0,dy:-1,dz:0,side:"down"}
# Internal: redstone torches on the neighbour underneath.
#
# This is the one side where a torch counts: a redstone torch strongly powers the
# block directly above it, and that block is us. Both shapes do it — a torch
# standing on the floor below and a wall torch mounted below — and for a wall
# torch the facing does not matter, because it is powering upwards, not sideways.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_torch[lit=true] run scoreboard players set #rs ra.temp 15
$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wall_torch[lit=true] run scoreboard players set #rs ra.temp 15
