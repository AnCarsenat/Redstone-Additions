# /ra_lib:transport/rebuild/expand
# Internal: breadth-first flood of the current network.
# One pass per wave, so the cost is proportional to the network's diameter rather
# than to a fixed guess at how far it might reach.

execute unless entity @e[type=marker,tag=ra.tr.frontier,limit=1] run return 0

tag @e[type=marker,tag=ra.tr.frontier] add ra.tr.wave
tag @e[type=marker,tag=ra.tr.frontier] remove ra.tr.frontier

execute as @e[type=marker,tag=ra.tr.wave] at @s run function ra_lib:transport/rebuild/spread
tag @e[type=marker,tag=ra.tr.wave] remove ra.tr.wave

function ra_lib:transport/rebuild/expand
