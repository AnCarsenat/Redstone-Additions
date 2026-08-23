# /ra_lib:transport/rebuild/seed
# Internal: start a new network at this node.
# The seed also becomes the network's root, which is where the contents get
# parked on the next rebuild.
#
# A NETWORK KEEPS ITS NUMBER
# Ids used to be handed out 1, 2, 3... on every rebuild, so they said nothing
# about identity: break one run and every run numbered above it slid down one.
# The Multimeter would report "Network 22", the run would be torn out, and
# `nets.n22` would still be sitting in storage afterwards holding somebody
# else's water -- which reads exactly like a network that refused to die.
#
# So a component seeded at a node that was the root of an old network inherits
# that network's number, and anything genuinely new takes the next number the
# counter has never issued. rebuild/assign_next is what makes this work: it seeds
# at old roots first, so every id that is going to be inherited is claimed before
# any fresh one is handed out.

scoreboard players set #next_net ra.tr.tmp 0
execute if score @s ra.tr.old matches 1.. run scoreboard players operation #next_net ra.tr.tmp = @s ra.tr.old
execute if score #next_net ra.tr.tmp matches ..0 run function ra_lib:transport/rebuild/next_id
execute if score #next_net ra.tr.tmp > #net_seq ra.tr.tmp run scoreboard players operation #net_seq ra.tr.tmp = #next_net ra.tr.tmp

scoreboard players operation @s ra.tr.net = #next_net ra.tr.tmp
tag @s add ra.tr.root
tag @s add ra.tr.frontier

execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/rebuild/reset_net with storage ra:transport arg
