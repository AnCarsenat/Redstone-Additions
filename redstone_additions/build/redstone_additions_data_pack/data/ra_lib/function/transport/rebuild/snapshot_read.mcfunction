# /ra_lib:transport/rebuild/snapshot_read {id:N}
# Internal: copy the network's amount and medium onto the root node, so both
# survive the rebuild that is about to discard every network.

$execute store result score @s ra.tr.carry run data get storage ra:transport nets.n$(id).amount
$execute if data storage ra:transport nets.n$(id).medium run data modify entity @s data.data.medium set from storage ra:transport nets.n$(id).medium
