# /ra_wires:electric/init_node
# Initialize missing electric-node fields with block-specific defaults.

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.transfer_rate run data modify entity @s data.properties.transfer_rate set value 60

execute unless data entity @s data.data.eu run data modify entity @s data.data.eu set value 0

execute if entity @s[tag=ra.custom_block.electric_wire] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 220
execute if entity @s[tag=ra.custom_block.electric_generator] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 700
execute if entity @s[tag=ra.custom_block.electric_consumer] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 220
execute if entity @s[tag=ra.custom_block.electric_switch] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 240
