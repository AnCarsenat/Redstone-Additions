# /ra_wires:bridge/consider {dx,dy,dz}
# Internal: weigh one neighbouring network against the best seen so far.
# Context: as that neighbour's node marker.
#
# Compared by HOW FULL, not by how much. A tank farm holding 2000 mL of its
# 300000 is nearly empty; a three-pipe stub holding 3000 of its 5000 is more than
# half full. Comparing raw amounts said the stub had more and called it level, so
# a valve between them sat there while the tanks stayed empty — which is exactly
# what a bank of tanks is for.
#
# Ten-thousandths rather than percent, because integer division throws away the
# remainder and a percent is too coarse to tell 0.4% from 0.9% — which is the
# difference between a tank farm filling and a tank farm looking full.

function ra_lib:transport/net/read
scoreboard players add #br.found ra.wires.tmp 1

scoreboard players set #br.fill ra.wires.tmp 0
scoreboard players set #br.scale ra.wires.tmp 10000
execute if score #net_capacity ra.tr.tmp matches 1.. run scoreboard players operation #br.fill ra.wires.tmp = #net_amount ra.tr.tmp
execute if score #net_capacity ra.tr.tmp matches 1.. run scoreboard players operation #br.fill ra.wires.tmp *= #br.scale ra.wires.tmp
execute if score #net_capacity ra.tr.tmp matches 1.. run scoreboard players operation #br.fill ra.wires.tmp /= #net_capacity ra.tr.tmp

$execute if score #br.fill ra.wires.tmp > #br.hi ra.wires.tmp run data modify storage ra:wires bridge.src set value {dx:$(dx),dy:$(dy),dz:$(dz)}
execute if score #br.fill ra.wires.tmp > #br.hi ra.wires.tmp run scoreboard players operation #br.hi_net ra.wires.tmp = @s ra.tr.net
execute if score #br.fill ra.wires.tmp > #br.hi ra.wires.tmp run scoreboard players operation #br.hi ra.wires.tmp = #br.fill ra.wires.tmp

$execute if score #br.fill ra.wires.tmp < #br.lo ra.wires.tmp run data modify storage ra:wires bridge.dst set value {dx:$(dx),dy:$(dy),dz:$(dz)}
execute if score #br.fill ra.wires.tmp < #br.lo ra.wires.tmp run scoreboard players operation #br.lo_net ra.wires.tmp = @s ra.tr.net
execute if score #br.fill ra.wires.tmp < #br.lo ra.wires.tmp run scoreboard players operation #br.lo ra.wires.tmp = #br.fill ra.wires.tmp
