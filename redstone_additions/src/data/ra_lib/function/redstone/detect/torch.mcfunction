# /ra_lib:redstone/detect/torch
# Internal: Detect lit redstone torches and wall torches. Power 15.

# Standing torches above/below this block
execute if block ~ ~-1 ~ minecraft:redstone_torch[lit=true] run tag @s add ra.powered.torch
execute if block ~ ~-1 ~ minecraft:redstone_torch[lit=true] run scoreboard players set @s ra.power.down 15
execute if block ~ ~1 ~ minecraft:redstone_torch[lit=true] run tag @s add ra.powered.torch
execute if block ~ ~1 ~ minecraft:redstone_torch[lit=true] run scoreboard players set @s ra.power.up 15

# Wall torch at z-1 powers us unless attached to us (facing=north)
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=south,lit=true] run tag @s add ra.powered.torch
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=south,lit=true] run scoreboard players set @s ra.power.north 15
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=east,lit=true] run tag @s add ra.powered.torch
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=east,lit=true] run scoreboard players set @s ra.power.north 15
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=west,lit=true] run tag @s add ra.powered.torch
execute if block ~ ~ ~-1 minecraft:redstone_wall_torch[facing=west,lit=true] run scoreboard players set @s ra.power.north 15

# Wall torch at z+1 powers us unless attached to us (facing=south)
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=north,lit=true] run tag @s add ra.powered.torch
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=north,lit=true] run scoreboard players set @s ra.power.south 15
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=east,lit=true] run tag @s add ra.powered.torch
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=east,lit=true] run scoreboard players set @s ra.power.south 15
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=west,lit=true] run tag @s add ra.powered.torch
execute if block ~ ~ ~1 minecraft:redstone_wall_torch[facing=west,lit=true] run scoreboard players set @s ra.power.south 15

# Wall torch at x-1 powers us unless attached to us (facing=west)
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=east,lit=true] run tag @s add ra.powered.torch
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=east,lit=true] run scoreboard players set @s ra.power.west 15
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=north,lit=true] run tag @s add ra.powered.torch
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=north,lit=true] run scoreboard players set @s ra.power.west 15
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=south,lit=true] run tag @s add ra.powered.torch
execute if block ~-1 ~ ~ minecraft:redstone_wall_torch[facing=south,lit=true] run scoreboard players set @s ra.power.west 15

# Wall torch at x+1 powers us unless attached to us (facing=east)
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=west,lit=true] run tag @s add ra.powered.torch
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=west,lit=true] run scoreboard players set @s ra.power.east 15
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=north,lit=true] run tag @s add ra.powered.torch
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=north,lit=true] run scoreboard players set @s ra.power.east 15
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=south,lit=true] run tag @s add ra.powered.torch
execute if block ~1 ~ ~ minecraft:redstone_wall_torch[facing=south,lit=true] run scoreboard players set @s ra.power.east 15
