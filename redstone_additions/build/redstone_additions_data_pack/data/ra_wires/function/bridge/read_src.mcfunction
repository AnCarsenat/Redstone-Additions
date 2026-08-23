# /ra_wires:bridge/read_src
# Internal: inspect the network behind the bridge.
# Context: as the node behind it. Writes #br.src / #br.src_amount / #br.src_net
# and parks the medium being moved in storage ra:wires bridge.medium.

function ra_lib:transport/net/read

execute if score #net_amount ra.tr.tmp matches ..0 run return 0

# WHICH MEDIUM MOVES
# A plain Valve or Breaker moves the network's primary -- whatever is at the
# front of its media list. A Liquid Filter moves only the medium it is set to,
# and is the reason this is a choice at all: it is the block that lets a mixed
# pipe run be separated back out into single-medium runs.
data modify storage ra:wires bridge.medium set from storage ra:transport cur.medium
execute if data storage ra:wires filter.medium run data modify storage ra:wires bridge.medium set from storage ra:wires filter.medium

# How much of THAT medium is here, not how much the network holds in total. A
# filter set to lava on a run holding 9000 water and 1000 lava has 1000 to move.
data modify storage ra:wires amtq set value {}
data modify storage ra:wires amtq.medium set from storage ra:wires bridge.medium
execute store result score #br.src_amount ra.wires.tmp run function ra_wires:bridge/read_amount with storage ra:wires amtq

execute if score #br.src_amount ra.wires.tmp matches ..0 run return 0

scoreboard players set #br.src ra.wires.tmp 1
scoreboard players operation #br.src_net ra.wires.tmp = @s ra.tr.net
