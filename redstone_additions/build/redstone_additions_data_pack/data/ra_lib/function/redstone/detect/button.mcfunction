# /ra_lib:redstone/detect/button
# Internal: Detect pressed buttons. Power 15.

# Stone buttons
execute if block ~ ~ ~-1 minecraft:stone_button[powered=true] run tag @s add ra.powered.button
execute if block ~ ~ ~-1 minecraft:stone_button[powered=true] run scoreboard players set @s ra.power.north 15
execute if block ~ ~ ~1 minecraft:stone_button[powered=true] run tag @s add ra.powered.button
execute if block ~ ~ ~1 minecraft:stone_button[powered=true] run scoreboard players set @s ra.power.south 15
execute if block ~-1 ~ ~ minecraft:stone_button[powered=true] run tag @s add ra.powered.button
execute if block ~-1 ~ ~ minecraft:stone_button[powered=true] run scoreboard players set @s ra.power.west 15
execute if block ~1 ~ ~ minecraft:stone_button[powered=true] run tag @s add ra.powered.button
execute if block ~1 ~ ~ minecraft:stone_button[powered=true] run scoreboard players set @s ra.power.east 15
execute if block ~ ~1 ~ minecraft:stone_button[powered=true] run tag @s add ra.powered.button
execute if block ~ ~1 ~ minecraft:stone_button[powered=true] run scoreboard players set @s ra.power.up 15
execute if block ~ ~-1 ~ minecraft:stone_button[powered=true] run tag @s add ra.powered.button
execute if block ~ ~-1 ~ minecraft:stone_button[powered=true] run scoreboard players set @s ra.power.down 15

# Oak buttons (and other wood types use same check pattern)
execute if block ~ ~ ~-1 #minecraft:buttons[powered=true] run tag @s add ra.powered.button
execute if block ~ ~ ~-1 #minecraft:buttons[powered=true] run scoreboard players set @s ra.power.north 15
execute if block ~ ~ ~1 #minecraft:buttons[powered=true] run tag @s add ra.powered.button
execute if block ~ ~ ~1 #minecraft:buttons[powered=true] run scoreboard players set @s ra.power.south 15
execute if block ~-1 ~ ~ #minecraft:buttons[powered=true] run tag @s add ra.powered.button
execute if block ~-1 ~ ~ #minecraft:buttons[powered=true] run scoreboard players set @s ra.power.west 15
execute if block ~1 ~ ~ #minecraft:buttons[powered=true] run tag @s add ra.powered.button
execute if block ~1 ~ ~ #minecraft:buttons[powered=true] run scoreboard players set @s ra.power.east 15
execute if block ~ ~1 ~ #minecraft:buttons[powered=true] run tag @s add ra.powered.button
execute if block ~ ~1 ~ #minecraft:buttons[powered=true] run scoreboard players set @s ra.power.up 15
execute if block ~ ~-1 ~ #minecraft:buttons[powered=true] run tag @s add ra.powered.button
execute if block ~ ~-1 ~ #minecraft:buttons[powered=true] run scoreboard players set @s ra.power.down 15
