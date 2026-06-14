# /ra_wires:liquid/pump_tick
# Generate liquid into pump buffer from facing source or configured medium

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.generation_rate run data modify entity @s data.properties.generation_rate set value 80
execute unless data entity @s data.properties.medium_id run data modify entity @s data.properties.medium_id set value 1
execute unless data entity @s data.data.medium_id run data modify entity @s data.data.medium_id set value 0
execute unless data entity @s data.data.amount run data modify entity @s data.data.amount set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 500

execute unless data entity @s data.properties{enabled:1b} run return 0

scoreboard players set #mid ra.wires.tmp 0
execute positioned ^ ^ ^1 if block ~ ~ ~ water[level=0] run scoreboard players set #mid ra.wires.tmp 1
execute positioned ^ ^ ^1 if block ~ ~ ~ lava[level=0] run scoreboard players set #mid ra.wires.tmp 2
execute positioned ^ ^ ^1 if block ~ ~ ~ powder_snow run scoreboard players set #mid ra.wires.tmp 5
execute if score #mid ra.wires.tmp matches 0 store result score #mid ra.wires.tmp run data get entity @s data.properties.medium_id 1

execute store result score #amt ra.wires.tmp2 run data get entity @s data.data.amount 1
execute store result score #cap ra.wires.tmp run data get entity @s data.data.capacity 1
execute store result score #cur_mid ra.wires.tmp2 run data get entity @s data.data.medium_id 1
execute store result score #gen ra.wires.tmp run data get entity @s data.properties.generation_rate 1

execute if score #mid ra.wires.tmp matches ..0 run return 0
execute if score #amt ra.wires.tmp2 matches 1.. unless score #cur_mid ra.wires.tmp2 matches 0 unless score #cur_mid ra.wires.tmp2 = #mid ra.wires.tmp run return 0

execute if score #amt ra.wires.tmp2 matches 0 run execute store result entity @s data.data.medium_id int 1 run scoreboard players get #mid ra.wires.tmp

scoreboard players operation #free ra.wires.tmp = #cap ra.wires.tmp
scoreboard players operation #free ra.wires.tmp -= #amt ra.wires.tmp2
execute if score #free ra.wires.tmp matches ..0 run return 0
execute if score #free ra.wires.tmp < #gen ra.wires.tmp run scoreboard players operation #gen ra.wires.tmp = #free ra.wires.tmp
execute if score #gen ra.wires.tmp matches ..0 run return 0

scoreboard players operation #amt ra.wires.tmp2 += #gen ra.wires.tmp
execute store result entity @s data.data.amount int 1 run scoreboard players get #amt ra.wires.tmp2
