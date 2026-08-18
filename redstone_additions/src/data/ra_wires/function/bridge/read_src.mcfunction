# /ra_wires:bridge/read_src
# Internal: inspect the network behind the bridge.
# Context: as the node behind it. Writes #br.src / #br.src_amount / #br.src_net
# and parks the medium in storage ra:wires bridge.medium.

function ra_lib:transport/net/read

execute if score #net_amount ra.tr.tmp matches ..0 run return 0

scoreboard players set #br.src ra.wires.tmp 1
scoreboard players operation #br.src_amount ra.wires.tmp = #net_amount ra.tr.tmp
scoreboard players operation #br.src_net ra.wires.tmp = @s ra.tr.net

# Electric grids carry "eu" and nothing else, so this is really for fluids: the
# far side has to be given the same medium it is losing, not whatever it happened
# to hold before.
data modify storage ra:wires bridge.medium set from storage ra:transport cur.medium
