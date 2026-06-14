# /ra_wires:liquid/update_medium_label

execute unless data entity @s data.data.medium_id run data modify entity @s data.data.medium_id set value 0
execute store result score #mid ra.wires.tmp run data get entity @s data.data.medium_id 1

execute if score #mid ra.wires.tmp matches 0 run data modify entity @s data.status.medium set value "Empty"
execute if score #mid ra.wires.tmp matches 1 run data modify entity @s data.status.medium set value "Water"
execute if score #mid ra.wires.tmp matches 2 run data modify entity @s data.status.medium set value "Lava"
execute if score #mid ra.wires.tmp matches 3 run data modify entity @s data.status.medium set value "XP"
execute if score #mid ra.wires.tmp matches 4 run data modify entity @s data.status.medium set value "Milk"
execute if score #mid ra.wires.tmp matches 5 run data modify entity @s data.status.medium set value "Powder Snow"
execute if score #mid ra.wires.tmp matches 11 run data modify entity @s data.status.medium set value "Steam"
execute if score #mid ra.wires.tmp matches 12 run data modify entity @s data.status.medium set value "Smoke"
execute if score #mid ra.wires.tmp matches 13 run data modify entity @s data.status.medium set value "Hydrogen"
execute if score #mid ra.wires.tmp matches 14 run data modify entity @s data.status.medium set value "Oxygen"
execute if score #mid ra.wires.tmp matches 15 run data modify entity @s data.status.medium set value "Chlorine"
