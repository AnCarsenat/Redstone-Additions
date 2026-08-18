# /ra_lib:transport/rebuild/accumulate {id:N}
# Internal: fold one node's capacity and carried contents into its new network.

$execute store result score #net_cap_acc ra.tr.tmp run data get storage ra:transport nets.n$(id).capacity
scoreboard players operation #net_cap_acc ra.tr.tmp += #node_cap ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).capacity int 1 run scoreboard players get #net_cap_acc ra.tr.tmp

$execute store result score #net_amt_acc ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
scoreboard players operation #net_amt_acc ra.tr.tmp += #node_carry ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amount int 1 run scoreboard players get #net_amt_acc ra.tr.tmp
