# /ra_wires:electric/update_status

execute unless data entity @s data.data.eu run data modify entity @s data.data.eu set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 0

data modify entity @s data.status.available_eu set from entity @s data.data.eu
data modify entity @s data.status.capacity set from entity @s data.data.capacity

execute store result score #eu ra.wires.tmp run data get entity @s data.data.eu 1
execute if score #eu ra.wires.tmp matches 1.. run data modify entity @s data.status.active set value 1b
execute if score #eu ra.wires.tmp matches ..0 run data modify entity @s data.status.active set value 0b
