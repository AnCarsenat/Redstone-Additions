# /ra_ender:link/receive_power
# Context: as the receiving vault marker, at its block.
# Takes as much of #ender.carry as its buffer has room for, and reports it in
# #ender.sent so the sender knows exactly what to deduct.

execute store result score #ender.dst ra.temp run data get entity @s data.data.eu
execute store result score #ender.cap ra.temp run data get entity @s data.data.capacity

scoreboard players operation #ender.room ra.temp = #ender.cap ra.temp
scoreboard players operation #ender.room ra.temp -= #ender.dst ra.temp
execute if score #ender.room ra.temp matches ..0 run return 0

scoreboard players operation #ender.sent ra.temp = #ender.carry ra.temp
execute if score #ender.sent ra.temp > #ender.room ra.temp run scoreboard players operation #ender.sent ra.temp = #ender.room ra.temp

scoreboard players operation #ender.dst ra.temp += #ender.sent ra.temp
execute store result entity @s data.data.eu int 1 run scoreboard players get #ender.dst ra.temp

playsound minecraft:block.beacon.activate block @a[distance=..8] ~ ~ ~ 0.12 1.9
particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 3
