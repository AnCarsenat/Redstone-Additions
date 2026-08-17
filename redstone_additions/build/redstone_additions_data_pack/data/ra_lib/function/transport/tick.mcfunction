# /ra_lib:transport/tick
# Rebuild networks when the topology changed. Called once per tick, before any
# module moves contents around.
#
# Debounced: a rebuild runs at most once every 5 ticks, so a player laying down a
# line of pipes does not trigger one flood fill per block placed.

scoreboard players remove #rebuild_cd ra.tr.tmp 1

execute unless data storage ra:transport {dirty:1b} run return 0
execute if score #rebuild_cd ra.tr.tmp matches 1.. run return 0

scoreboard players set #rebuild_cd ra.tr.tmp 5
data modify storage ra:transport dirty set value 0b

function ra_lib:transport/rebuild/run
