# /ra_wires:bridge/read_dst
# Internal: inspect the network in front of the bridge.
# Context: as the node in front. Writes #br.dst / #br.dst_free / #br.dst_net.
#
# A network that already holds something else refuses the transfer outright
# rather than having it refused later by net/offer, after the source has already
# been debited.

function ra_lib:transport/net/read

scoreboard players operation #br.dst_free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #br.dst_free ra.wires.tmp -= #net_amount ra.tr.tmp
execute if score #br.dst_free ra.wires.tmp matches ..0 run return 0

# An empty network takes anything; a full one only takes more of what it has.
execute if score #net_amount ra.tr.tmp matches 1.. unless data storage ra:transport cur.medium run return 0
execute if score #net_amount ra.tr.tmp matches 1.. run function ra_wires:bridge/match_medium with storage ra:transport cur

execute if score #net_amount ra.tr.tmp matches 1.. if score #br.dst ra.wires.tmp matches 0 run return 0

scoreboard players set #br.dst ra.wires.tmp 1
scoreboard players operation #br.dst_amount ra.wires.tmp = #net_amount ra.tr.tmp
scoreboard players operation #br.dst_net ra.wires.tmp = @s ra.tr.net
