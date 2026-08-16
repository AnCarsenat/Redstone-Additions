# /ra_lib:transport/net/handover
# Internal: pass root status to any surviving member of the network.
# If there is no survivor the contents are gone, which is the right outcome —
# the last node of that network was just removed.

scoreboard players set #handover ra.tr.tmp 0
execute as @e[type=marker,tag=ra.tr.node] if score @s ra.tr.net = #leave_net ra.tr.tmp run function ra_lib:transport/net/handover_pick
