# /ra_lib:transport/net/read_run {id:N}
# Internal: the dynamic-name half of net/read.

$scoreboard players operation #net_amount ra.tr.tmp = net$(id) ra.tr.amount
$scoreboard players operation #net_capacity ra.tr.tmp = net$(id) ra.tr.capacity
$execute if data storage ra:transport nets.n$(id).medium run data modify storage ra:transport cur.medium set from storage ra:transport nets.n$(id).medium
