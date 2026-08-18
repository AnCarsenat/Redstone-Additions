# /ra_lib:transport/net/take_write {id:N}
# Internal: commit a withdrawal. A network drained to nothing forgets its medium,
# so a different one can be pumped in afterwards without the player having to
# break anything.

$execute store result score #net_new ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
scoreboard players operation #net_new ra.tr.tmp -= #net_want ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amount int 1 run scoreboard players get #net_new ra.tr.tmp
$execute if score #net_new ra.tr.tmp matches ..0 run data remove storage ra:transport nets.n$(id).medium
