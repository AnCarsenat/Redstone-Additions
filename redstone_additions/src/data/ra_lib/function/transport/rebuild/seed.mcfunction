# /ra_lib:transport/rebuild/seed
# Internal: start a new network at this node.
# The seed also becomes the network's root, which is where the contents get
# parked on the next rebuild.

scoreboard players operation @s ra.tr.net = #next_net ra.tr.tmp
tag @s add ra.tr.root
tag @s add ra.tr.frontier

execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/rebuild/reset_net with storage ra:transport arg
