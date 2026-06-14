# /ra_wires:liquid/update_status

execute unless data entity @s data.data.amount run data modify entity @s data.data.amount set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 0

data modify entity @s data.status.amount set from entity @s data.data.amount
data modify entity @s data.status.capacity set from entity @s data.data.capacity

function ra_wires:liquid/update_medium_label
