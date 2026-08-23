# /ra_lib:redstone/dust/side {dx:0,dy:0,dz:-1,back:"south"}
# Internal: redstone dust on a horizontal neighbour. Leaves the level in
# #rs ra.temp.
#
# Dust only counts when it is pointing at us: a line running past the side does
# not power what it passes. `$(back)` is the connection state on the neighbour's
# side that faces us, and `up` is the same connection climbing a block.
#
# Only a horizontal neighbour has a connection state naming us. Dust above and
# below is a different rule entirely -- see dust/above and dust/none.

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=side] run function ra_lib:redstone/analog {dx:$(dx),dy:$(dy),dz:$(dz)}
$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=up] run function ra_lib:redstone/analog {dx:$(dx),dy:$(dy),dz:$(dz)}
