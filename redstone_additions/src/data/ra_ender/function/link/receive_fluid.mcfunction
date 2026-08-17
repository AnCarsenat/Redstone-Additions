# /ra_ender:link/receive_fluid {medium:"..."}
# Context: as the receiving vault marker, at its block.

execute store result storage ra:ender offer.amount int 1 run scoreboard players get #ender.carry ra.temp
data modify storage ra:ender offer.medium set from storage ra:ender fluid.medium

function ra_ender:link/offer_fluid with storage ra:ender offer
scoreboard players operation #ender.carry ra.temp -= #net_moved ra.tr.tmp

execute if score #net_moved ra.tr.tmp matches 1.. run playsound minecraft:entity.enderman.teleport block @a[distance=..8] ~ ~ ~ 0.2 1.9
execute if score #net_moved ra.tr.tmp matches 1.. run particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 4
