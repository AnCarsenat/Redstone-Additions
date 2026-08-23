# /ra_wires:bridge/read_dst
# Internal: inspect the network in front of the bridge.
# Context: as the node in front. Writes #br.dst / #br.dst_free / #br.dst_net and
# #br.dst_amount, the latter being how much of the medium being MOVED is already
# there.
#
# It no longer refuses a network holding something else. Networks hold several
# media at once now, so "already holds something else" is not a refusal any more
# -- the only thing that can turn a transfer away is having no room, and room is
# the sum of everything in there against the capacity.

function ra_lib:transport/net/read

scoreboard players operation #br.dst_free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #br.dst_free ra.wires.tmp -= #net_amount ra.tr.tmp
execute if score #br.dst_free ra.wires.tmp matches ..0 run return 0

# Compared per medium, so the half-the-gap convergence in bridge/transfer levels
# the thing being moved rather than the totals. Two runs each holding 5000, one
# all water and one all lava, are not level from a water valve's point of view.
data modify storage ra:wires amtq set value {}
data modify storage ra:wires amtq.medium set from storage ra:wires bridge.medium
execute store result score #br.dst_amount ra.wires.tmp run function ra_wires:bridge/read_amount with storage ra:wires amtq

scoreboard players set #br.dst ra.wires.tmp 1
scoreboard players operation #br.dst_net ra.wires.tmp = @s ra.tr.net
