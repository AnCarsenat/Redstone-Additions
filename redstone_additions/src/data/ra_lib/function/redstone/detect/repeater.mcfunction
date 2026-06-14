# /ra_lib:redstone/detect/repeater
# Internal: Detect powered repeaters outputting into this block. Superpower 16.

# North (repeater at z-1 facing south = outputting into us)
execute if block ~ ~ ~-1 minecraft:repeater[facing=south,powered=true] run tag @s add ra.powered.repeater
execute if block ~ ~ ~-1 minecraft:repeater[facing=south,powered=true] run scoreboard players set @s ra.power.north 16

# South (repeater at z+1 facing north)
execute if block ~ ~ ~1 minecraft:repeater[facing=north,powered=true] run tag @s add ra.powered.repeater
execute if block ~ ~ ~1 minecraft:repeater[facing=north,powered=true] run scoreboard players set @s ra.power.south 16

# West (repeater at x-1 facing east)
execute if block ~-1 ~ ~ minecraft:repeater[facing=east,powered=true] run tag @s add ra.powered.repeater
execute if block ~-1 ~ ~ minecraft:repeater[facing=east,powered=true] run scoreboard players set @s ra.power.west 16

# East (repeater at x+1 facing west)
execute if block ~1 ~ ~ minecraft:repeater[facing=west,powered=true] run tag @s add ra.powered.repeater
execute if block ~1 ~ ~ minecraft:repeater[facing=west,powered=true] run scoreboard players set @s ra.power.east 16
