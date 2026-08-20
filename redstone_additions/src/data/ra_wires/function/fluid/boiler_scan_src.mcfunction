# /ra_wires:fluid/boiler_scan_src
# Internal: claim the first adjacent network holding enough water.
# Context: as a neighbouring fluid node.

execute if score #boil_src ra.wires.tmp matches 1 run return 0

function ra_lib:transport/net/read
# Enough WATER, not enough of whatever the network calls its primary.
execute store result score #boil_water ra.wires.tmp run data get storage ra:transport cur.amounts.water
execute unless score #boil_water ra.wires.tmp matches 1000.. run return 0

scoreboard players set #boil_src ra.wires.tmp 1
scoreboard players operation #boil_src_net ra.wires.tmp = @s ra.tr.net
tag @s add ra.wires.boil_src
