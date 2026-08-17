# /ra_infinite:blocks/poppy_generator/plant
# Put one random flower here. Context: at a spot already checked for ground.

execute store result score #gen.roll ra.temp run random value 1..8
execute if score #gen.roll ra.temp matches 1 run setblock ~ ~ ~ minecraft:poppy
execute if score #gen.roll ra.temp matches 2 run setblock ~ ~ ~ minecraft:dandelion
execute if score #gen.roll ra.temp matches 3 run setblock ~ ~ ~ minecraft:cornflower
execute if score #gen.roll ra.temp matches 4 run setblock ~ ~ ~ minecraft:azure_bluet
execute if score #gen.roll ra.temp matches 5 run setblock ~ ~ ~ minecraft:oxeye_daisy
execute if score #gen.roll ra.temp matches 6 run setblock ~ ~ ~ minecraft:allium
execute if score #gen.roll ra.temp matches 7 run setblock ~ ~ ~ minecraft:blue_orchid
execute if score #gen.roll ra.temp matches 8 run setblock ~ ~ ~ minecraft:lily_of_the_valley

particle minecraft:happy_villager ~ ~0.4 ~ 0.3 0.3 0.3 0.01 4
playsound minecraft:block.grass.place block @a[distance=..12] ~ ~ ~ 0.3 1.3

# Single mode stops searching once something has been planted.
scoreboard players set #poppy.done ra.temp 1
