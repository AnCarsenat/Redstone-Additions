# /ra_lib:transport/rebuild/run
# Recompute every transport network from scratch.
#
# Only reached when the topology actually changed. The contents of the old
# networks are carried over: each network keeps one seed node tagged ra.tr.root,
# and the network's contents are parked on that node for the duration of the
# rebuild, then summed back into whichever new network the node lands in.

# --- Park the old contents on each network's root ---
scoreboard players set @e[type=marker,tag=ra.tr.node] ra.tr.carry 0
execute as @e[type=marker,tag=ra.tr.root] run function ra_lib:transport/rebuild/snapshot

# --- Forget the old assignment ---
scoreboard players set @e[type=marker,tag=ra.tr.node] ra.tr.net 0
tag @e[type=marker,tag=ra.tr.root] remove ra.tr.root
scoreboard players set #next_net ra.tr.tmp 0
data modify storage ra:transport nets set value {}

# --- Assign one network per connected component ---
function ra_lib:transport/rebuild/assign_next

# --- Total capacity and carried contents per network ---
execute as @e[type=marker,tag=ra.tr.node] run function ra_lib:transport/rebuild/accumulate_node

# --- Adopt a medium for networks that received contents ---
execute as @e[type=marker,tag=ra.tr.node,scores={ra.tr.carry=1..}] run function ra_lib:transport/rebuild/adopt_medium

# A network can inherit more than its new capacity holds if a tank was removed.
execute as @e[type=marker,tag=ra.tr.root] run function ra_lib:transport/rebuild/clamp

# Carried values are consumed; clear them so a later rebuild cannot double-count.
scoreboard players set @e[type=marker,tag=ra.tr.node] ra.tr.carry 0
data remove storage ra:transport arg
