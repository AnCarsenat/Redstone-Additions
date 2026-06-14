# /ra_wires:liquid/init_node
# Initialize missing fluid-node fields with block-specific defaults.

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.transfer_rate run data modify entity @s data.properties.transfer_rate set value 50

execute unless data entity @s data.data.amount run data modify entity @s data.data.amount set value 0
execute unless data entity @s data.data.medium_id run data modify entity @s data.data.medium_id set value 0

execute if entity @s[tag=ra.custom_block.liquid_pipe] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 200
execute if entity @s[tag=ra.custom_block.liquid_tank] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 4000
execute if entity @s[tag=ra.custom_block.liquid_pump] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 500
execute if entity @s[tag=ra.custom_block.liquid_valve] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 300
execute if entity @s[tag=ra.custom_block.liquid_drain] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 600

execute if entity @s[tag=ra.custom_block.gas_tank] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 3000
execute if entity @s[tag=ra.custom_block.gas_pump] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 500
execute if entity @s[tag=ra.custom_block.gas_valve] unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 280
