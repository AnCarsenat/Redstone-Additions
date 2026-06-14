# /ra_gates:blocks/randomizer/trigger
# Trigger random output based on chance property

# Get chance property (default 50 if not set)
execute unless data entity @s data.properties.chance run data modify entity @s data.properties.chance set value 50
execute store result score @s ra.temp run data get entity @s data.properties.chance

# Clamp chance to 0-100
execute if score @s ra.temp matches ..0 run scoreboard players set @s ra.temp 0
execute if score @s ra.temp matches 101.. run scoreboard players set @s ra.temp 100

# Generate random number 0-99 and store in ra.power (on self)
execute store result score @s ra.power run random value 0..99

# Output: if random < chance, output ON
execute if score @s ra.power < @s ra.temp at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute unless score @s ra.power < @s ra.temp at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# Particle feedback
execute if score @s ra.power < @s ra.temp run particle minecraft:happy_villager ~ ~1 ~ 0.3 0.3 0.3 0.02 5
execute unless score @s ra.power < @s ra.temp run particle minecraft:smoke ~ ~1 ~ 0.3 0.3 0.3 0.02 5
