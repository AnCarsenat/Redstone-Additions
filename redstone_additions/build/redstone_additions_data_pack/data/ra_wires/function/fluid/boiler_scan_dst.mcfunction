# /ra_wires:fluid/boiler_scan_dst
# Internal: claim the first adjacent network that can take the steam.
# Context: as a neighbouring fluid node.

execute if score #boil_dst ra.wires.tmp matches 1 run return 0

# Not the network the water is coming from — that one is full of water.
execute if entity @s[tag=ra.wires.boil_src] run return 0
execute if score @s ra.tr.net = #boil_src_net ra.wires.tmp run return 0

function ra_lib:transport/net/read
execute if score #net_amount ra.tr.tmp matches 1.. unless data storage ra:transport cur{medium:"steam"} run return 0

scoreboard players operation #boil_free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #boil_free ra.wires.tmp -= #net_amount ra.tr.tmp
execute unless score #boil_free ra.wires.tmp matches 100.. run return 0

scoreboard players set #boil_dst ra.wires.tmp 1
tag @s add ra.wires.boil_dst
