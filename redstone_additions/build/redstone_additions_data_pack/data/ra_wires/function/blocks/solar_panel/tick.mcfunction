# /ra_wires:blocks/solar_panel/tick
# Generate EU from sunlight.
# Context: as the solar panel marker, at the panel position.
#
# The panel IS a vanilla daylight detector, so the game has already worked out
# how much sky light reaches it and published the answer as the block's `power`
# state. Reading that is free and handles night, rain, roofs, snow cover and the
# Nether without a single predicate of our own.

execute unless data entity @s data.properties{enabled:1b} run return 0

scoreboard players add @s ra.cooldown 1
execute unless score @s ra.cooldown matches 20.. run return 0
scoreboard players set @s ra.cooldown 0

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
execute if score #solar_gen ra.wires.tmp matches 0 run return 0

data modify entity @s data.status.fuel set value "Sunlight"
data modify entity @s data.status.active set value 1b

execute store result score #solar_eu ra.wires.tmp2 run data get entity @s data.data.eu 1
execute store result score #solar_cap ra.wires.tmp run data get entity @s data.data.capacity 1

scoreboard players operation #solar_free ra.wires.tmp = #solar_cap ra.wires.tmp
scoreboard players operation #solar_free ra.wires.tmp -= #solar_eu ra.wires.tmp2
execute if score #solar_free ra.wires.tmp matches ..0 run return 0
execute if score #solar_free ra.wires.tmp < #solar_gen ra.wires.tmp run scoreboard players operation #solar_gen ra.wires.tmp = #solar_free ra.wires.tmp

scoreboard players operation #solar_eu ra.wires.tmp2 += #solar_gen ra.wires.tmp
execute store result entity @s data.data.eu int 1 run scoreboard players get #solar_eu ra.wires.tmp2
