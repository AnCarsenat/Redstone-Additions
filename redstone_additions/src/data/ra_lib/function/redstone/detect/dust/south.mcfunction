# /ra_lib:redstone/detect/dust/south
# Internal: resolve the exact power (1-15) of the redstone dust at z+1.
# Only reached once the caller has confirmed that the dust is connected
# toward this block and is not at power 0, so the tag always applies.

tag @s add ra.powered.dust

execute if block ~ ~ ~1 minecraft:redstone_wire[power=1] run scoreboard players set @s ra.power.south 1
execute if block ~ ~ ~1 minecraft:redstone_wire[power=2] run scoreboard players set @s ra.power.south 2
execute if block ~ ~ ~1 minecraft:redstone_wire[power=3] run scoreboard players set @s ra.power.south 3
execute if block ~ ~ ~1 minecraft:redstone_wire[power=4] run scoreboard players set @s ra.power.south 4
execute if block ~ ~ ~1 minecraft:redstone_wire[power=5] run scoreboard players set @s ra.power.south 5
execute if block ~ ~ ~1 minecraft:redstone_wire[power=6] run scoreboard players set @s ra.power.south 6
execute if block ~ ~ ~1 minecraft:redstone_wire[power=7] run scoreboard players set @s ra.power.south 7
execute if block ~ ~ ~1 minecraft:redstone_wire[power=8] run scoreboard players set @s ra.power.south 8
execute if block ~ ~ ~1 minecraft:redstone_wire[power=9] run scoreboard players set @s ra.power.south 9
execute if block ~ ~ ~1 minecraft:redstone_wire[power=10] run scoreboard players set @s ra.power.south 10
execute if block ~ ~ ~1 minecraft:redstone_wire[power=11] run scoreboard players set @s ra.power.south 11
execute if block ~ ~ ~1 minecraft:redstone_wire[power=12] run scoreboard players set @s ra.power.south 12
execute if block ~ ~ ~1 minecraft:redstone_wire[power=13] run scoreboard players set @s ra.power.south 13
execute if block ~ ~ ~1 minecraft:redstone_wire[power=14] run scoreboard players set @s ra.power.south 14
execute if block ~ ~ ~1 minecraft:redstone_wire[power=15] run scoreboard players set @s ra.power.south 15
