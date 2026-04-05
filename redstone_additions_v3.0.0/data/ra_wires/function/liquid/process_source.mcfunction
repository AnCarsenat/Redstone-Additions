# /ra_wires:liquid/process_source
# Try to push liquid from this node to adjacent nodes

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.transfer_rate run data modify entity @s data.properties.transfer_rate set value 50
execute unless data entity @s data.data.amount run data modify entity @s data.data.amount set value 0
execute unless data entity @s data.data.medium_id run data modify entity @s data.data.medium_id set value 0

execute unless data entity @s data.properties{enabled:1b} run return 0
execute store result score #src_amt ra.wires.tmp run data get entity @s data.data.amount 1
execute if score #src_amt ra.wires.tmp matches ..0 run return 0

tag @s add ra.wires.liq_src
function ra_wires:liquid/transfer_adjacent
tag @s remove ra.wires.liq_src
