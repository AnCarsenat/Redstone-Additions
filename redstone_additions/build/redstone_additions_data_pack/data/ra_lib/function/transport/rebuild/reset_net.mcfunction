# /ra_lib:transport/rebuild/reset_net {id:N}
# Internal: zero a freshly created network's totals.

$scoreboard players set net$(id) ra.tr.amount 0
$scoreboard players set net$(id) ra.tr.capacity 0
$data modify storage ra:transport nets.n$(id) set value {}
