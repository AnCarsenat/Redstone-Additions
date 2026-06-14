# /ra_wires:liquid/drain_tick
# Drain nearby liquid where possible, else show fallback particles

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.medium_id run data modify entity @s data.properties.medium_id set value 1
execute unless data entity @s data.data.medium_id run data modify entity @s data.data.medium_id set value 0
execute unless data entity @s data.data.amount run data modify entity @s data.data.amount set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 600

execute unless data entity @s data.properties{enabled:1b} run return 0

scoreboard players set #mid ra.wires.tmp 0
scoreboard players set #vol ra.wires.tmp2 0

execute positioned ^ ^ ^1 if block ~ ~ ~ water[level=0] run scoreboard players set #mid ra.wires.tmp 1
execute positioned ^ ^ ^1 if block ~ ~ ~ water[level=0] run scoreboard players set #vol ra.wires.tmp2 1000

execute positioned ^ ^ ^1 if block ~ ~ ~ lava[level=0] run scoreboard players set #mid ra.wires.tmp 2
execute positioned ^ ^ ^1 if block ~ ~ ~ lava[level=0] run scoreboard players set #vol ra.wires.tmp2 1000

execute positioned ^ ^ ^1 if block ~ ~ ~ powder_snow run scoreboard players set #mid ra.wires.tmp 5
execute positioned ^ ^ ^1 if block ~ ~ ~ powder_snow run scoreboard players set #vol ra.wires.tmp2 1000

execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=3] run scoreboard players set #mid ra.wires.tmp 1
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=3] run scoreboard players set #vol ra.wires.tmp2 333
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=2] run scoreboard players set #mid ra.wires.tmp 1
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=2] run scoreboard players set #vol ra.wires.tmp2 333
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=1] run scoreboard players set #mid ra.wires.tmp 1
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=1] run scoreboard players set #vol ra.wires.tmp2 333

execute positioned ^ ^ ^1 if block ~ ~ ~ lava_cauldron run scoreboard players set #mid ra.wires.tmp 2
execute positioned ^ ^ ^1 if block ~ ~ ~ lava_cauldron run scoreboard players set #vol ra.wires.tmp2 1000

execute if score #mid ra.wires.tmp matches ..0 run data modify entity @s data.status.drain_state set value "no_source"
execute if score #mid ra.wires.tmp matches ..0 run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 8
execute if score #mid ra.wires.tmp matches ..0 run return 0

execute store result score #amt ra.wires.tmp2 run data get entity @s data.data.amount 1
execute store result score #cap ra.wires.tmp run data get entity @s data.data.capacity 1
execute store result score #cur_mid ra.wires.tmp2 run data get entity @s data.data.medium_id 1

execute if score #amt ra.wires.tmp2 matches 1.. unless score #cur_mid ra.wires.tmp2 matches 0 unless score #cur_mid ra.wires.tmp2 = #mid ra.wires.tmp run data modify entity @s data.status.drain_state set value "blocked"
execute if score #amt ra.wires.tmp2 matches 1.. unless score #cur_mid ra.wires.tmp2 matches 0 unless score #cur_mid ra.wires.tmp2 = #mid ra.wires.tmp run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 8
execute if score #amt ra.wires.tmp2 matches 1.. unless score #cur_mid ra.wires.tmp2 matches 0 unless score #cur_mid ra.wires.tmp2 = #mid ra.wires.tmp run return 0

execute if score #amt ra.wires.tmp2 matches 0 run execute store result entity @s data.data.medium_id int 1 run scoreboard players get #mid ra.wires.tmp

scoreboard players operation #free ra.wires.tmp = #cap ra.wires.tmp
scoreboard players operation #free ra.wires.tmp -= #amt ra.wires.tmp2
execute if score #free ra.wires.tmp matches ..0 run data modify entity @s data.status.drain_state set value "full"
execute if score #free ra.wires.tmp matches ..0 run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 8
execute if score #free ra.wires.tmp matches ..0 run return 0

execute if score #free ra.wires.tmp < #vol ra.wires.tmp2 run scoreboard players operation #vol ra.wires.tmp2 = #free ra.wires.tmp
execute if score #vol ra.wires.tmp2 matches ..0 run data modify entity @s data.status.drain_state set value "full"
execute if score #vol ra.wires.tmp2 matches ..0 run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 8
execute if score #vol ra.wires.tmp2 matches ..0 run return 0

# Perform world drain
execute positioned ^ ^ ^1 if block ~ ~ ~ water[level=0] run setblock ~ ~ ~ air
execute positioned ^ ^ ^1 if block ~ ~ ~ lava[level=0] run setblock ~ ~ ~ air
execute positioned ^ ^ ^1 if block ~ ~ ~ powder_snow run setblock ~ ~ ~ air
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=1] run setblock ~ ~ ~ cauldron
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=2] run setblock ~ ~ ~ water_cauldron[level=1]
execute positioned ^ ^ ^1 if block ~ ~ ~ water_cauldron[level=3] run setblock ~ ~ ~ water_cauldron[level=2]
execute positioned ^ ^ ^1 if block ~ ~ ~ lava_cauldron run setblock ~ ~ ~ cauldron

scoreboard players operation #amt ra.wires.tmp2 += #vol ra.wires.tmp2
execute store result entity @s data.data.amount int 1 run scoreboard players get #amt ra.wires.tmp2

data modify entity @s data.status.drain_state set value "drained"
particle minecraft:splash ~ ~0.8 ~ 0.2 0.2 0.2 0.01 8
