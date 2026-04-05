# /ra_wires:gas/pump_tick
# Generate configured gas into pump buffer

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.generation_rate run data modify entity @s data.properties.generation_rate set value 70
execute unless data entity @s data.properties.medium_id run data modify entity @s data.properties.medium_id set value 11
execute unless data entity @s data.data.medium_id run data modify entity @s data.data.medium_id set value 0
execute unless data entity @s data.data.amount run data modify entity @s data.data.amount set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 500

execute unless data entity @s data.properties{enabled:1b} run return 0

execute store result score #mid ra.wires.tmp run data get entity @s data.properties.medium_id 1
execute if score #mid ra.wires.tmp matches 1..5 run scoreboard players add #mid ra.wires.tmp 10
execute if score #mid ra.wires.tmp matches 11..15 run execute store result entity @s data.properties.medium_id int 1 run scoreboard players get #mid ra.wires.tmp
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
