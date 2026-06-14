
# /ra_multiblock:drill/tick
# Per-tick processing for drill multiblock
# Context: as multiblock marker, at base position
# Dispatches to tick_facing macro

# Skip if disabled
execute unless data entity @s data.properties{enabled:1b} run return 0

# Dispatch tick logic by facing
execute if data entity @s data{facing:"north"} if block ~ ~ ~-1 minecraft:redstone_block run return 0
execute if data entity @s data{facing:"north"} run particle minecraft:smoke ~ ~1 ~ 0.5 0.2 0.5 0.1 5 force
execute if data entity @s data{facing:"north"} run data modify storage ra:temp bf.result set value {id:"minecraft:command_block",count:1,components:{"minecraft:item_name":"Rock","minecraft:item_model":"dead_brain_coral_block","minecraft:rarity":"common"}}
execute if data entity @s data{facing:"north"} positioned ~1 ~ ~ run function ra_lib:inventory/insert with storage ra:temp bf.result

execute if data entity @s data{facing:"south"} if block ~ ~ ~1 minecraft:redstone_block run return 0
execute if data entity @s data{facing:"south"} run particle minecraft:smoke ~ ~1 ~ 0.5 0.2 0.5 0.1 5 force
execute if data entity @s data{facing:"south"} run data modify storage ra:temp bf.result set value {id:"minecraft:command_block",count:1,components:{"minecraft:item_name":"Rock","minecraft:item_model":"dead_brain_coral_block","minecraft:rarity":"common"}}
execute if data entity @s data{facing:"south"} positioned ~-1 ~ ~ run function ra_lib:inventory/insert with storage ra:temp bf.result

execute if data entity @s data{facing:"east"} if block ~1 ~ ~ minecraft:redstone_block run return 0
execute if data entity @s data{facing:"east"} run particle minecraft:smoke ~ ~1 ~ 0.5 0.2 0.5 0.1 5 force
execute if data entity @s data{facing:"east"} run data modify storage ra:temp bf.result set value {id:"minecraft:command_block",count:1,components:{"minecraft:item_name":"Rock","minecraft:item_model":"dead_brain_coral_block","minecraft:rarity":"common"}}
execute if data entity @s data{facing:"east"} positioned ~ ~ ~1 run function ra_lib:inventory/insert with storage ra:temp bf.result

execute if data entity @s data{facing:"west"} if block ~-1 ~ ~ minecraft:redstone_block run return 0
execute if data entity @s data{facing:"west"} run particle minecraft:smoke ~ ~1 ~ 0.5 0.2 0.5 0.1 5 force
execute if data entity @s data{facing:"west"} run data modify storage ra:temp bf.result set value {id:"minecraft:command_block",count:1,components:{"minecraft:item_name":"Rock","minecraft:item_model":"dead_brain_coral_block","minecraft:rarity":"common"}}
execute if data entity @s data{facing:"west"} positioned ~ ~ ~-1 run function ra_lib:inventory/insert with storage ra:temp bf.result
