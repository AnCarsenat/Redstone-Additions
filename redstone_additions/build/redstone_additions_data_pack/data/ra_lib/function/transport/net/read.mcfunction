# /ra_lib:transport/net/read
# Load this node's network state into scratch.
# Context: as a node marker.
# Output: #net_amount / #net_capacity ra.tr.tmp — the TOTAL across every medium —
#         and storage ra:transport cur:
#           cur.medium   the primary medium (absent when the network is empty)
#           cur.amounts  {water:5000,lava:2000}
#           cur.media    [{m:"water"},{m:"lava"}]

scoreboard players set #net_amount ra.tr.tmp 0
scoreboard players set #net_capacity ra.tr.tmp 0
data remove storage ra:transport cur

execute if score @s ra.tr.net matches ..0 run return 0

execute store result storage ra:transport rq.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/net/read_run with storage ra:transport rq
