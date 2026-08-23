# /ra_lib:transport/rebuild/assign_next
# Internal: seed a new network on the next still-unassigned node, flood it, and
# repeat until every node belongs to one.
#
# OLD ROOTS GO FIRST
# Seeding at a node that used to be a root is what lets rebuild/seed give the
# component that node's old number back. Doing all of those before any new
# component is seeded also means every inherited number is claimed before the
# counter issues a fresh one, so the two can never land on the same id.

execute unless entity @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},limit=1] run return 0

execute as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0,ra.tr.old=1..},limit=1] run function ra_lib:transport/rebuild/seed
execute unless entity @e[type=marker,tag=ra.tr.frontier,limit=1] as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},limit=1] run function ra_lib:transport/rebuild/seed

function ra_lib:transport/rebuild/expand

function ra_lib:transport/rebuild/assign_next
