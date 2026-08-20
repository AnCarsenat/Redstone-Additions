# /ra_lib:redstone/dust_any/side {dx:0,dy:0,dz:-1,back:"south"}
# Internal: is the dust on this horizontal neighbour carrying anything?
# Returns 1 or 0. The cheap twin of ra_lib:redstone/dust/side -- testing against
# power=0 answers in one comparison where resolving the level costs fifteen.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=side] unless block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[power=0] run return 1
$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=up] unless block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[power=0] run return 1
return 0
