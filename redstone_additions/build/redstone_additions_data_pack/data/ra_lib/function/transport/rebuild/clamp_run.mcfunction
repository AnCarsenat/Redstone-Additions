# /ra_lib:transport/rebuild/clamp_run {id:N}
# Internal: the dynamic-name half of clamp.

$execute if score net$(id) ra.tr.amount > net$(id) ra.tr.capacity run scoreboard players operation net$(id) ra.tr.amount = net$(id) ra.tr.capacity
$execute if score net$(id) ra.tr.amount matches ..0 run data remove storage ra:transport nets.n$(id).medium
