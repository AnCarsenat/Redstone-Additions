# /ra_lib:transport/rebuild/absorb_one {id:N,m:"..."}
# Internal: add one carried medium to the network's breakdown.
# #abs.a ra.tr.tmp holds how much of it there is.
#
# The same shape as net/offer_write, and for the same reason: `data get` on a key
# that is not there fails, a failed command stores 0, and 0 is exactly the right
# starting value for a medium arriving on this network for the first time. A new
# medium is appended to the list, so the order still records what got here first.

execute if score #abs.a ra.tr.tmp matches ..0 run return 0

$execute store result score #abs.cur ra.tr.tmp run data get storage ra:transport nets.n$(id).amounts.$(m)
$execute if score #abs.cur ra.tr.tmp matches ..0 run data modify storage ra:transport nets.n$(id).media append value {m:"$(m)"}

scoreboard players operation #abs.cur ra.tr.tmp += #abs.a ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amounts.$(m) int 1 run scoreboard players get #abs.cur ra.tr.tmp

$data modify storage ra:transport nets.n$(id).medium set from storage ra:transport nets.n$(id).media[0].m
