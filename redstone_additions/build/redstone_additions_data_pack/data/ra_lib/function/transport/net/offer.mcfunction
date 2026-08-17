# /ra_lib:transport/net/offer {amount:N,medium:"water"}
# Push contents into this node's network.
# Context: as a node marker.
# Returns how much was actually accepted, which may be less than asked for, or 0
# when the network is full or already holds a different medium.

scoreboard players set #net_moved ra.tr.tmp 0
execute if score @s ra.tr.net matches ..0 run return 0

function ra_lib:transport/net/read

# A network holding something else will not take this.
$execute if score #net_amount ra.tr.tmp matches 1.. unless data storage ra:transport cur{medium:"$(medium)"} run return 0

$scoreboard players set #net_want ra.tr.tmp $(amount)
scoreboard players operation #net_free ra.tr.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #net_free ra.tr.tmp -= #net_amount ra.tr.tmp

execute if score #net_free ra.tr.tmp matches ..0 run return 0
execute if score #net_free ra.tr.tmp < #net_want ra.tr.tmp run scoreboard players operation #net_want ra.tr.tmp = #net_free ra.tr.tmp
execute if score #net_want ra.tr.tmp matches ..0 run return 0

execute store result storage ra:transport wq.id int 1 run scoreboard players get @s ra.tr.net
$data modify storage ra:transport wq.medium set value "$(medium)"
function ra_lib:transport/net/offer_write with storage ra:transport wq

scoreboard players operation #net_moved ra.tr.tmp = #net_want ra.tr.tmp
return run scoreboard players get #net_moved ra.tr.tmp
