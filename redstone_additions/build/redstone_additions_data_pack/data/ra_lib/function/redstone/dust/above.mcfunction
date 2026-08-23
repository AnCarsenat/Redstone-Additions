# /ra_lib:redstone/dust/above {dx:0,dy:1,dz:0}
# Internal: redstone dust on the neighbour directly above. Leaves the level in
# #rs ra.temp.
#
# Dust powers the block it is lying on, whichever way the line happens to run --
# that is how a lamp under a dust line lights. So there is no connection test
# here, and there cannot be one: redstone_wire has only north, south, east and
# west connection states, and no `down`. Asking it for one is what broke this
# side for every source, not just dust; see dust/none.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire run function ra_lib:redstone/analog {dx:$(dx),dy:$(dy),dz:$(dz)}
