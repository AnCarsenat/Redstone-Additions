# /ra_infinite:blocks/poppy_generator/process
# Plant in front of the generator — one flower, or a patch.
# Context: as the generator's marker, at the generator, rotated with it.

execute store result score #gen.period ra.temp run data get entity @s data.properties.cooldown 1
execute unless score @s ra.cooldown >= #gen.period ra.temp run return 0
scoreboard players set @s ra.cooldown 0

execute if data entity @s data.properties{enabled:0b} run return 0

execute if data entity @s data.properties{mode:"patch"} positioned ^ ^ ^1 run function ra_infinite:blocks/poppy_generator/patch
execute unless data entity @s data.properties{mode:"patch"} positioned ^ ^ ^1 run function ra_infinite:blocks/poppy_generator/single
