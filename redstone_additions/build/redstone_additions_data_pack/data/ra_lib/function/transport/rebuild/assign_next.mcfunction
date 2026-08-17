# /ra_lib:transport/rebuild/assign_next
# Internal: seed a new network on the next still-unassigned node, flood it, and
# repeat until every node belongs to one.

execute unless entity @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},limit=1] run return 0

scoreboard players add #next_net ra.tr.tmp 1

execute as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},limit=1] run function ra_lib:transport/rebuild/seed
function ra_lib:transport/rebuild/expand

function ra_lib:transport/rebuild/assign_next
