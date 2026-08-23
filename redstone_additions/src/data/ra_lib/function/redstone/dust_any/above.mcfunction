# /ra_lib:redstone/dust_any/above {dx:0,dy:1,dz:0}
# Internal: is the dust directly above carrying anything? Returns 1 or 0.
# The cheap twin of ra_lib:redstone/dust/above -- no connection test, because
# dust powers whatever it lies on.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire unless block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[power=0] run return 1
return 0
