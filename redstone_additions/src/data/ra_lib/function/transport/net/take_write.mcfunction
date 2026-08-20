# /ra_lib:transport/net/take_write {id:N,medium:"..."}
# Internal: commit a withdrawal, from the total and from the medium's own entry.
#
# A medium drained to nothing leaves the network entirely -- its key goes and so
# does its place in the media list -- so a pipe run that once carried lava does
# not keep claiming to until something else displaces it. A network drained to
# nothing forgets everything, which is what lets a different medium be pumped in
# afterwards without the player breaking anything.

$execute store result score #net_new ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
scoreboard players operation #net_new ra.tr.tmp -= #net_want ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amount int 1 run scoreboard players get #net_new ra.tr.tmp

$execute store result score #net_med ra.tr.tmp run data get storage ra:transport nets.n$(id).amounts.$(medium)
scoreboard players operation #net_med ra.tr.tmp -= #net_want ra.tr.tmp

$execute if score #net_med ra.tr.tmp matches 1.. store result storage ra:transport nets.n$(id).amounts.$(medium) int 1 run scoreboard players get #net_med ra.tr.tmp
$execute if score #net_med ra.tr.tmp matches ..0 run data remove storage ra:transport nets.n$(id).amounts.$(medium)
$execute if score #net_med ra.tr.tmp matches ..0 run data remove storage ra:transport nets.n$(id).media[{m:"$(medium)"}]

# The primary is whatever is left at the front of the list. Removed first so that
# an emptied network is left with no medium at all rather than a stale one: the
# `set from` below fails silently on an empty list and would leave the old value
# standing.
$data remove storage ra:transport nets.n$(id).medium
$execute if score #net_new ra.tr.tmp matches 1.. run data modify storage ra:transport nets.n$(id).medium set from storage ra:transport nets.n$(id).media[0].m
