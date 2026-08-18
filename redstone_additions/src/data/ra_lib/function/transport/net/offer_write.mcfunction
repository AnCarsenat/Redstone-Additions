# /ra_lib:transport/net/offer_write {id:N,medium:"..."}
# Internal: commit an accepted offer.

$execute store result score #net_new ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
scoreboard players operation #net_new ra.tr.tmp += #net_want ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amount int 1 run scoreboard players get #net_new ra.tr.tmp
$data modify storage ra:transport nets.n$(id).medium set value "$(medium)"
