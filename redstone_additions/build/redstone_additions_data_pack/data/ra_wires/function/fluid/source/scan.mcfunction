# /ra_wires:fluid/source/scan
# Find a drainable world source in one of the six adjacent blocks.
# Context: at the node position.
# Output: storage ra:wires found = {match,medium,volume,drained,dx,dy,dz}
#         cleared when nothing is adjacent.
#
# The old pump only ever looked at ^ ^ ^1 — local coordinates, on a marker placed
# with dir_type:0 whose rotation was therefore always the default. In practice
# that meant a pump could only ever see the block to its south, which is why
# pumps looked like they did nothing. Six-way adjacency removes the dependency on
# a facing the block never really had.

data remove storage ra:wires found

execute positioned ~1 ~ ~ run function ra_wires:fluid/source/probe {dx:1,dy:0,dz:0}
execute positioned ~-1 ~ ~ run function ra_wires:fluid/source/probe {dx:-1,dy:0,dz:0}
execute positioned ~ ~ ~1 run function ra_wires:fluid/source/probe {dx:0,dy:0,dz:1}
execute positioned ~ ~ ~-1 run function ra_wires:fluid/source/probe {dx:0,dy:0,dz:-1}
execute positioned ~ ~-1 ~ run function ra_wires:fluid/source/probe {dx:0,dy:-1,dz:0}
execute positioned ~ ~1 ~ run function ra_wires:fluid/source/probe {dx:0,dy:1,dz:0}
