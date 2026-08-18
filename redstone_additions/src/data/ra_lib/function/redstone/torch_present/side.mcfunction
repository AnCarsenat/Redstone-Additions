# /ra_lib:redstone/torch_present/side {dx:0,dy:0,dz:-1,side:"north"}
# Internal: is there a wall torch on this horizontal neighbour that could power
# us? Presence only — lit or not. Mirrors ra_lib:redstone/torch/side, minus the
# lit test, so the two must be kept in step.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wall_torch unless block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wall_torch[facing=$(side)] run return 1
return 0
