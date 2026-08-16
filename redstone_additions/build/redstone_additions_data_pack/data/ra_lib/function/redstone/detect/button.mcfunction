# /ra_lib:redstone/detect/button
# Internal: Detect pressed buttons. Power 15.

# #minecraft:buttons covers every button, stone included — one pass is enough.
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
