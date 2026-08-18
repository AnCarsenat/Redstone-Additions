# /ra_ender:link/receive_power
# Context: as the receiving vault marker, at its block.
# Offers #ender.carry to the receiving vault's GRID and reports what was taken in
# #ender.sent, so the sender knows exactly what to keep and what to refund.
#
# The receiver has no buffer of its own any more. What arrives goes onto its grid
# — into that base's batteries, or straight into whatever is drawing there on the
# same tick — and a grid with no room simply accepts less.

scoreboard players set #ender.sent ra.temp 0

execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ender.carry ra.temp
execute store result score #ender.sent ra.temp run function ra_wires:electric/offer_eu with storage ra:wires eu

execute if score #ender.sent ra.temp matches ..0 run return 0

playsound minecraft:block.beacon.activate block @a[distance=..8] ~ ~ ~ 0.12 1.9
particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 3
