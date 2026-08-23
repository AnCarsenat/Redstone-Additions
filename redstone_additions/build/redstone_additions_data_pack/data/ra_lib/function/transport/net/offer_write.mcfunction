# /ra_lib:transport/net/offer_write {id:N,medium:"..."}
# Internal: commit an accepted offer, to the total and to the medium's own entry.
#
# `data get` on a path that is not there fails, and a failed command stores 0 --
# which is exactly the right starting value for a medium arriving on this network
# for the first time.

$execute store result score #net_new ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
scoreboard players operation #net_new ra.tr.tmp += #net_want ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amount int 1 run scoreboard players get #net_new ra.tr.tmp

$execute store result score #net_med ra.tr.tmp run data get storage ra:transport nets.n$(id).amounts.$(medium)

# First arrival of this medium: give it a place in the enumeration order. The
# list is compounds rather than bare strings so that a medium can be removed by
# value when it runs out -- `data remove list[{m:"water"}]` only works on
# compound elements.
$execute if score #net_med ra.tr.tmp matches ..0 run data modify storage ra:transport nets.n$(id).media append value {m:"$(medium)"}

scoreboard players operation #net_med ra.tr.tmp += #net_want ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amounts.$(medium) int 1 run scoreboard players get #net_med ra.tr.tmp

$data modify storage ra:transport nets.n$(id).medium set from storage ra:transport nets.n$(id).media[0].m
