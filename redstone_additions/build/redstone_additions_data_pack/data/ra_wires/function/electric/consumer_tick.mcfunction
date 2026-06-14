# /ra_wires:electric/consumer_tick
# Draw EU each tick and expose active status

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.eu_use run data modify entity @s data.properties.eu_use set value 40
execute unless data entity @s data.data.eu run data modify entity @s data.data.eu set value 0

execute unless data entity @s data.properties{enabled:1b} run data modify entity @s data.status.active set value 0b
execute unless data entity @s data.properties{enabled:1b} run return 0

execute store result score #eu ra.wires.tmp run data get entity @s data.data.eu 1
execute store result score #use ra.wires.tmp2 run data get entity @s data.properties.eu_use 1

execute if score #eu ra.wires.tmp < #use ra.wires.tmp2 run data modify entity @s data.status.active set value 0b
execute if score #eu ra.wires.tmp < #use ra.wires.tmp2 run return 0

scoreboard players operation #eu ra.wires.tmp -= #use ra.wires.tmp2
execute store result entity @s data.data.eu int 1 run scoreboard players get #eu ra.wires.tmp

data modify entity @s data.status.active set value 1b
particle minecraft:electric_spark ~ ~0.9 ~ 0.25 0.25 0.25 0.01 2
