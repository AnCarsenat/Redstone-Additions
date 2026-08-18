# /ra_wires:bridge/scan
# Internal: find the fullest and emptiest networks touching this block, and move
# contents from one to the other. Context: as the bridge marker, at its block.
#
# Six neighbours, no facing. Whichever adjacent network holds the most gives to
# whichever holds the least — so a valve dropped into a pipe run works whichever
# way round it was placed, and a bridge at a junction still does the sensible
# thing rather than picking two spaces at random.

scoreboard players set #br.hi ra.wires.tmp -1
scoreboard players set #br.lo ra.wires.tmp 2147483647
scoreboard players set #br.found ra.wires.tmp 0
data remove storage ra:wires bridge

execute positioned ~1 ~ ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/consider {dx:1,dy:0,dz:0}
execute positioned ~-1 ~ ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/consider {dx:-1,dy:0,dz:0}
execute positioned ~ ~ ~1 as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/consider {dx:0,dy:0,dz:1}
execute positioned ~ ~ ~-1 as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/consider {dx:0,dy:0,dz:-1}
execute positioned ~ ~1 ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/consider {dx:0,dy:1,dz:0}
execute positioned ~ ~-1 ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/consider {dx:0,dy:-1,dz:0}

execute if score #br.found ra.wires.tmp matches ..1 run return run data modify entity @s data.status.bridge_state set value "needs_two_networks"

# Both extremes landing on the same network means everything touching this block
# is already one network -- pipes on both sides that meet somewhere behind it.
# There is nothing to move, and reporting throughput would be a lie.
execute if score #br.hi_net ra.wires.tmp = #br.lo_net ra.wires.tmp run return run data modify entity @s data.status.bridge_state set value "same_network"

execute if score #br.hi ra.wires.tmp = #br.lo ra.wires.tmp run return run function ra_wires:bridge/balanced

data modify storage ra:wires move set value {}
data modify storage ra:wires move.sx set from storage ra:wires bridge.src.dx
data modify storage ra:wires move.sy set from storage ra:wires bridge.src.dy
data modify storage ra:wires move.sz set from storage ra:wires bridge.src.dz
data modify storage ra:wires move.dx set from storage ra:wires bridge.dst.dx
data modify storage ra:wires move.dy set from storage ra:wires bridge.dst.dy
data modify storage ra:wires move.dz set from storage ra:wires bridge.dst.dz
function ra_wires:bridge/transfer with storage ra:wires move
data remove storage ra:wires move
