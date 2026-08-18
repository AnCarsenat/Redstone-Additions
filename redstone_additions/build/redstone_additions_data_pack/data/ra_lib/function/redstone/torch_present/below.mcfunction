# /ra_lib:redstone/torch_present/below {dx:0,dy:-1,dz:0,side:"down"}
# Internal: is there a torch underneath that could power us? Presence only.
# Mirrors ra_lib:redstone/torch/below minus the lit test.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_torch run return 1
$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wall_torch run return 1
return 0
