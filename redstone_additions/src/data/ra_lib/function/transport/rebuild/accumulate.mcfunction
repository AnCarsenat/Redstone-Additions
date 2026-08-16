# /ra_lib:transport/rebuild/accumulate {id:N}
# Internal: the scoreboard side of accumulate_node.

$scoreboard players operation net$(id) ra.tr.capacity += #node_cap ra.tr.tmp
$scoreboard players operation net$(id) ra.tr.amount += #node_carry ra.tr.tmp
