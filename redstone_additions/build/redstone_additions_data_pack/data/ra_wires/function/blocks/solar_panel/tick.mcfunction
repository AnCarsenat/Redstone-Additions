# /ra_wires:blocks/solar_panel/tick
# Generate EU from sunlight and put it on the grid.
# Context: as the solar panel marker, at the panel position.
#
# The panel IS a vanilla daylight detector, so the game has already worked out
# how much sky light reaches it and published the answer as the block's `power`
# state. Reading that is free and handles night, rain, roofs, snow cover and the
# Nether without a single predicate of our own.
#
# THE TABLE BELOW IS EU PER TICK, AND IT IS OFFERED EVERY TICK
# It used to be gated behind a 20-tick duty cycle, which quietly divided the
# panel's output by twenty: the scale peaks at 50, so a panel made 2.5 EU/t and
# twenty of them made 50 EU/t at absolute noon. Every number the pack states
# about this block assumes otherwise -- the Electric Furnace's whole mode table
# is priced against "a Solar Panel peaks at 50 EU/t, one EU Generator is 60"
# (see electric_furnace/read_mode), and the changelog puts twenty panels at
# ~200 EU/t averaged over a daylight cycle. Both are only true at one offer per
# tick.
#
# The visible symptom was an Electric Furnace on `superpowered` -- 300 EU every
# 5 ticks, 60 EU/t -- outrunning twenty panels' 50 EU/t for ever, so the grid sat
# at zero and the Batteries next to it never charged however much sun there was.
#
# Batching it back up instead (20x the amount, once every 20 ticks) would average
# the same and behave worse: a 1000 EU burst needs somewhere to land on the tick
# it arrives, and a panel contributes only 50 of capacity, so most of it would be
# refused outright on any grid without a Battery.

scoreboard players set #solar_gen ra.wires.tmp 0
execute if block ~ ~ ~ minecraft:daylight_detector[power=5] run scoreboard players set #solar_gen ra.wires.tmp 10
execute if block ~ ~ ~ minecraft:daylight_detector[power=6] run scoreboard players set #solar_gen ra.wires.tmp 12
execute if block ~ ~ ~ minecraft:daylight_detector[power=7] run scoreboard players set #solar_gen ra.wires.tmp 15
execute if block ~ ~ ~ minecraft:daylight_detector[power=8] run scoreboard players set #solar_gen ra.wires.tmp 18
execute if block ~ ~ ~ minecraft:daylight_detector[power=9] run scoreboard players set #solar_gen ra.wires.tmp 22
execute if block ~ ~ ~ minecraft:daylight_detector[power=10] run scoreboard players set #solar_gen ra.wires.tmp 26
execute if block ~ ~ ~ minecraft:daylight_detector[power=11] run scoreboard players set #solar_gen ra.wires.tmp 30
execute if block ~ ~ ~ minecraft:daylight_detector[power=12] run scoreboard players set #solar_gen ra.wires.tmp 35
execute if block ~ ~ ~ minecraft:daylight_detector[power=13] run scoreboard players set #solar_gen ra.wires.tmp 40
execute if block ~ ~ ~ minecraft:daylight_detector[power=14] run scoreboard players set #solar_gen ra.wires.tmp 45
execute if block ~ ~ ~ minecraft:daylight_detector[power=15] run scoreboard players set #solar_gen ra.wires.tmp 50

execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.fuel set value "No sunlight"
execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.active set value 0b
execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.output set value 0
execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.accepted set value 0
execute if score #solar_gen ra.wires.tmp matches 0 run return 0

data modify entity @s data.status.fuel set value "Sunlight"

# Offer the batch to the grid rather than into a buffer of our own. The network
# refuses whatever will not fit, so there is no free-space arithmetic here and no
# capacity of ours to overflow.
execute store result storage ra:wires eu.amount int 1 run scoreboard players get #solar_gen ra.wires.tmp
execute store result score #solar_made ra.wires.tmp run function ra_wires:electric/offer_eu with storage ra:wires eu

execute if score #solar_made ra.wires.tmp matches 1.. run data modify entity @s data.status.active set value 1b
execute if score #solar_made ra.wires.tmp matches ..0 run data modify entity @s data.status.active set value 0b

# What the panel is MAKING, which is not what the grid took -- a full grid
# refuses a panel in full sun and the two numbers are worth telling apart. The EU
# Generator publishes the same pair for the same reason.
execute store result entity @s data.status.output int 1 run scoreboard players get #solar_gen ra.wires.tmp
execute store result entity @s data.status.accepted int 1 run scoreboard players get #solar_made ra.wires.tmp

