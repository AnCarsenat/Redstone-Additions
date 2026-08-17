# /ra_lib:transport/rebuild/claim
# Internal: add an unassigned neighbour to the network being flooded.

scoreboard players operation @s ra.tr.net = #next_net ra.tr.tmp
tag @s add ra.tr.frontier
