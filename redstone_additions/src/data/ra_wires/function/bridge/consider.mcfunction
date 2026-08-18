# /ra_wires:bridge/consider {dx,dy,dz}
# Internal: weigh one neighbouring network against the best seen so far.
# Context: as that neighbour's node marker.

function ra_lib:transport/net/read
scoreboard players add #br.found ra.wires.tmp 1

$execute if score #net_amount ra.tr.tmp > #br.hi ra.wires.tmp run data modify storage ra:wires bridge.src set value {dx:$(dx),dy:$(dy),dz:$(dz)}
execute if score #net_amount ra.tr.tmp > #br.hi ra.wires.tmp run scoreboard players operation #br.hi_net ra.wires.tmp = @s ra.tr.net
execute if score #net_amount ra.tr.tmp > #br.hi ra.wires.tmp run scoreboard players operation #br.hi ra.wires.tmp = #net_amount ra.tr.tmp

$execute if score #net_amount ra.tr.tmp < #br.lo ra.wires.tmp run data modify storage ra:wires bridge.dst set value {dx:$(dx),dy:$(dy),dz:$(dz)}
execute if score #net_amount ra.tr.tmp < #br.lo ra.wires.tmp run scoreboard players operation #br.lo_net ra.wires.tmp = @s ra.tr.net
execute if score #net_amount ra.tr.tmp < #br.lo ra.wires.tmp run scoreboard players operation #br.lo ra.wires.tmp = #net_amount ra.tr.tmp
