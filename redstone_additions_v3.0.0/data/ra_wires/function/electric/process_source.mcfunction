# /ra_wires:electric/process_source
# Try to push EU from this node to adjacent electric nodes

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.transfer_rate run data modify entity @s data.properties.transfer_rate set value 60
execute unless data entity @s data.data.eu run data modify entity @s data.data.eu set value 0

execute unless data entity @s data.properties{enabled:1b} run return 0
execute store result score #src_eu ra.wires.tmp run data get entity @s data.data.eu 1
execute if score #src_eu ra.wires.tmp matches ..0 run return 0

tag @s add ra.wires.eu_src
function ra_wires:electric/transfer_adjacent
tag @s remove ra.wires.eu_src
