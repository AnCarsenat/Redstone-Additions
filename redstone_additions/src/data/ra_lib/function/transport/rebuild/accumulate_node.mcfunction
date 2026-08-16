# /ra_lib:transport/rebuild/accumulate_node
# Internal: fold one node's capacity and carried contents into its new network.

execute store result score #node_cap ra.tr.tmp run scoreboard players get @s ra.tr.cap
execute store result score #node_carry ra.tr.tmp run scoreboard players get @s ra.tr.carry
execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/rebuild/accumulate with storage ra:transport arg
