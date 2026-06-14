# /ra_lib:redstone/detect/block
# Internal: Detect redstone blocks. Always power 15.

execute if block ~ ~ ~-1 minecraft:redstone_block run tag @s add ra.powered.block
execute if block ~ ~ ~-1 minecraft:redstone_block run scoreboard players set @s ra.power.north 15

execute if block ~ ~ ~1 minecraft:redstone_block run tag @s add ra.powered.block
execute if block ~ ~ ~1 minecraft:redstone_block run scoreboard players set @s ra.power.south 15

execute if block ~-1 ~ ~ minecraft:redstone_block run tag @s add ra.powered.block
execute if block ~-1 ~ ~ minecraft:redstone_block run scoreboard players set @s ra.power.west 15

execute if block ~1 ~ ~ minecraft:redstone_block run tag @s add ra.powered.block
execute if block ~1 ~ ~ minecraft:redstone_block run scoreboard players set @s ra.power.east 15

execute if block ~ ~1 ~ minecraft:redstone_block run tag @s add ra.powered.block
execute if block ~ ~1 ~ minecraft:redstone_block run scoreboard players set @s ra.power.up 15

execute if block ~ ~-1 ~ minecraft:redstone_block run tag @s add ra.powered.block
execute if block ~ ~-1 ~ minecraft:redstone_block run scoreboard players set @s ra.power.down 15
