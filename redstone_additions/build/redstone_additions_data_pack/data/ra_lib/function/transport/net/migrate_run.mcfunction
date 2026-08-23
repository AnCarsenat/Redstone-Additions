# /ra_lib:transport/net/migrate_run {id:N,medium:"water"}
# Internal: give a pre-multi-medium network the breakdown it never had.
# Everything it holds is the one medium it was recorded as holding, so the whole
# amount moves into that key and the media list gets its single entry.

$data modify storage ra:transport nets.n$(id).media set value [{m:"$(medium)"}]
$data modify storage ra:transport nets.n$(id).amounts set value {}
$execute store result score #mig.amt ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
$execute if score #mig.amt ra.tr.tmp matches 1.. store result storage ra:transport nets.n$(id).amounts.$(medium) int 1 run scoreboard players get #mig.amt ra.tr.tmp
