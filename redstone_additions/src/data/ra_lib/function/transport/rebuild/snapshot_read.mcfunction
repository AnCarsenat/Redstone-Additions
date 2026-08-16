# /ra_lib:transport/rebuild/snapshot_read {id:N}
# Internal: copy the network's amount and medium onto the root node.

$execute store result score @s ra.tr.carry run scoreboard players get net$(id) ra.tr.amount
$execute if data storage ra:transport nets.n$(id).medium run data modify entity @s data.data.medium set from storage ra:transport nets.n$(id).medium
