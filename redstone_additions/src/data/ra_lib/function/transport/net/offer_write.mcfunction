# /ra_lib:transport/net/offer_write {id:N,medium:"..."}
# Internal: commit an accepted offer.

$scoreboard players operation net$(id) ra.tr.amount += #net_want ra.tr.tmp
$data modify storage ra:transport nets.n$(id).medium set value "$(medium)"
