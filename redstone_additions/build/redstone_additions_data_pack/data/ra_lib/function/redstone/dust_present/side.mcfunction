# /ra_lib:redstone/dust_present/side {dx:0,dy:0,dz:-1,back:"south"}
# Internal: is there dust on this horizontal neighbour pointing at us, powered
# or not? Returns 1 or 0. Mirrors ra_lib:redstone/dust/side minus the level.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=side] run return 1
$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=up] run return 1
return 0
