# /ra_lib:transport/net/take_write {id:N}
# Internal: commit a withdrawal. A network drained to nothing forgets its medium,
# so a different one can be pumped in afterwards without the player having to
# break anything.

$scoreboard players operation net$(id) ra.tr.amount -= #net_want ra.tr.tmp
$execute if score net$(id) ra.tr.amount matches ..0 run data remove storage ra:transport nets.n$(id).medium
