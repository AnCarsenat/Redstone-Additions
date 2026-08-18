# /ra_wires:bridge/transfer {sx,sy,sz,dx,dy,dz}
# Internal: move contents from the source side to the destination side.
# Context: as the bridge marker, at its block. bridge/scan has already picked the
# fullest adjacent network as the source and the emptiest as the destination.
#
# Both ends are inspected before anything moves, for the same reason the Boiler
# does it: taking first and discovering afterwards that the far side is full or
# holds something else would delete the difference. Nothing here half-succeeds.

scoreboard players set #br.src ra.wires.tmp 0
scoreboard players set #br.dst ra.wires.tmp 0
scoreboard players set #br.move ra.wires.tmp 0
data remove storage ra:wires bridge

function ra_lib:util/property {name:"rate",default:1000,min:1}
scoreboard players operation #br.rate ra.wires.tmp = #prop ra.temp

$execute positioned ~$(sx) ~$(sy) ~$(sz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/read_src
$execute positioned ~$(dx) ~$(dy) ~$(dz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/read_dst

execute if score #br.dst ra.wires.tmp matches 0 run return run data modify entity @s data.status.bridge_state set value "blocked"

scoreboard players operation #br.move ra.wires.tmp = #br.rate ra.wires.tmp
execute if score #br.src_amount ra.wires.tmp < #br.move ra.wires.tmp run scoreboard players operation #br.move ra.wires.tmp = #br.src_amount ra.wires.tmp
execute if score #br.dst_free ra.wires.tmp < #br.move ra.wires.tmp run scoreboard players operation #br.move ra.wires.tmp = #br.dst_free ra.wires.tmp

# Never move more than HALF the difference between the two sides.
#
# Moving the whole difference does not level them, it swaps them: 3000 and 2000
# becomes 2000 and 3000, and the next tick moves it all back. That is what a valve
# reporting a steady "Moving: 1000 mL" between two sides that never changed was
# actually doing — ping-ponging the same litre for ever.
#
# Half converges instead, and once the two are within one unit the halved gap
# rounds to zero and nothing moves at all.
scoreboard players operation #br.gap ra.wires.tmp = #br.src_amount ra.wires.tmp
scoreboard players operation #br.gap ra.wires.tmp -= #br.dst_amount ra.wires.tmp
scoreboard players set #br.two ra.wires.tmp 2
scoreboard players operation #br.gap ra.wires.tmp /= #br.two ra.wires.tmp
execute if score #br.gap ra.wires.tmp < #br.move ra.wires.tmp run scoreboard players operation #br.move ra.wires.tmp = #br.gap ra.wires.tmp

execute if score #br.move ra.wires.tmp matches ..0 run return run data modify entity @s data.status.bridge_state set value "blocked"

execute store result storage ra:wires bridge.amount int 1 run scoreboard players get #br.move ra.wires.tmp

$execute positioned ~$(sx) ~$(sy) ~$(sz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/take with storage ra:wires bridge
$execute positioned ~$(dx) ~$(dy) ~$(dz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ra_wires:bridge/give with storage ra:wires bridge

data modify entity @s data.status.bridge_state set value "transferring"
execute store result entity @s data.status.moved int 1 run scoreboard players get #br.move ra.wires.tmp
