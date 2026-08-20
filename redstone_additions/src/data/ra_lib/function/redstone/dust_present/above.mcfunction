# /ra_lib:redstone/dust_present/above {dx:0,dy:1,dz:0}
# Internal: is there dust directly above, powered or not? Returns 1 or 0.
# Mirrors ra_lib:redstone/dust/above minus the level.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire run return 1
return 0
