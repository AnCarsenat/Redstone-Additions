# /ra_lib:transport/net/handover_pick
# Internal: first candidate wins.

execute if score #handover ra.tr.tmp matches 1 run return 0
scoreboard players set #handover ra.tr.tmp 1
tag @s add ra.tr.root
