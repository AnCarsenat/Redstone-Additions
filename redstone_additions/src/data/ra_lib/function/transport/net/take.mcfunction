# /ra_lib:transport/net/take {amount:N}
# Pull contents out of this node's network.
# Context: as a node marker.
# Returns how much was actually removed. Check the medium with net/read first if
# the caller cares what it is getting.

scoreboard players set #net_moved ra.tr.tmp 0
execute if score @s ra.tr.net matches ..0 run return 0

function ra_lib:transport/net/read

$scoreboard players set #net_want ra.tr.tmp $(amount)
execute if score #net_amount ra.tr.tmp < #net_want ra.tr.tmp run scoreboard players operation #net_want ra.tr.tmp = #net_amount ra.tr.tmp
execute if score #net_want ra.tr.tmp matches ..0 run return 0

execute store result storage ra:transport wq.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/net/take_write with storage ra:transport wq

scoreboard players operation #net_moved ra.tr.tmp = #net_want ra.tr.tmp
return run scoreboard players get #net_moved ra.tr.tmp
