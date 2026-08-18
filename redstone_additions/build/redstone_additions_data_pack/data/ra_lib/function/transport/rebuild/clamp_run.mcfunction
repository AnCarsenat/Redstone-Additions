# /ra_lib:transport/rebuild/clamp_run {id:N}
# Internal: the dynamic-name half of clamp.

$execute store result score #net_amt_c ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
$execute store result score #net_cap_c ra.tr.tmp run data get storage ra:transport nets.n$(id).capacity

execute if score #net_amt_c ra.tr.tmp > #net_cap_c ra.tr.tmp run scoreboard players operation #net_amt_c ra.tr.tmp = #net_cap_c ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amount int 1 run scoreboard players get #net_amt_c ra.tr.tmp

$execute if score #net_amt_c ra.tr.tmp matches ..0 run data remove storage ra:transport nets.n$(id).medium
