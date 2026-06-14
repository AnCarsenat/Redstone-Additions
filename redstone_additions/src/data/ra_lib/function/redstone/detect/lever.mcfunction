# /ra_lib:redstone/detect/lever
# Internal: Detect adjacent levers. Power 15 when on.

# Adjacent levers (on ground/wall next to block)
execute if block ~ ~ ~-1 minecraft:lever[powered=true] run tag @s add ra.powered.lever
execute if block ~ ~ ~-1 minecraft:lever[powered=true] run scoreboard players set @s ra.power.north 15

execute if block ~ ~ ~1 minecraft:lever[powered=true] run tag @s add ra.powered.lever
execute if block ~ ~ ~1 minecraft:lever[powered=true] run scoreboard players set @s ra.power.south 15

execute if block ~-1 ~ ~ minecraft:lever[powered=true] run tag @s add ra.powered.lever
execute if block ~-1 ~ ~ minecraft:lever[powered=true] run scoreboard players set @s ra.power.west 15

execute if block ~1 ~ ~ minecraft:lever[powered=true] run tag @s add ra.powered.lever
execute if block ~1 ~ ~ minecraft:lever[powered=true] run scoreboard players set @s ra.power.east 15

execute if block ~ ~1 ~ minecraft:lever[powered=true] run tag @s add ra.powered.lever
execute if block ~ ~1 ~ minecraft:lever[powered=true] run scoreboard players set @s ra.power.up 15

execute if block ~ ~-1 ~ minecraft:lever[powered=true] run tag @s add ra.powered.lever
execute if block ~ ~-1 ~ minecraft:lever[powered=true] run scoreboard players set @s ra.power.down 15
