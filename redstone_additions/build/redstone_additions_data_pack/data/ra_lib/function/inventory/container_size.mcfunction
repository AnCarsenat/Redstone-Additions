# /ra_lib:inventory/container_size
# Slot count of the container at the current position, into #inv_max ra.temp.
# Defaults to 27, which covers chests, barrels and shulker boxes.

scoreboard players set #inv_max ra.temp 27

execute if block ~ ~ ~ minecraft:hopper run scoreboard players set #inv_max ra.temp 5
execute if block ~ ~ ~ minecraft:brewing_stand run scoreboard players set #inv_max ra.temp 5
execute if block ~ ~ ~ minecraft:dropper run scoreboard players set #inv_max ra.temp 9
execute if block ~ ~ ~ minecraft:dispenser run scoreboard players set #inv_max ra.temp 9
execute if block ~ ~ ~ minecraft:crafter run scoreboard players set #inv_max ra.temp 9
execute if block ~ ~ ~ minecraft:furnace run scoreboard players set #inv_max ra.temp 3
execute if block ~ ~ ~ minecraft:blast_furnace run scoreboard players set #inv_max ra.temp 3
execute if block ~ ~ ~ minecraft:smoker run scoreboard players set #inv_max ra.temp 3
