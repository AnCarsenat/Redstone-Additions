# /ra_infinite:blocks/nether_generator/process
# Grow one block of nether stone in front of the generator.
# Context: as the generator's marker, at the generator, rotated with it.

# A missing cooldown used to read as zero here, and zero means "long enough"
# to the comparison below -- the generator then ran every tick.
function ra_lib:util/property {name:"cooldown",default:100,min:1}
execute unless score @s ra.cooldown >= #prop ra.temp run return 0
scoreboard players set @s ra.cooldown 0

execute if data entity @s data.properties{enabled:0b} run return 0

execute unless block ^ ^ ^1 #ra_infinite:growable run return 0

# 1% ancient debris on a 120-tick period is one scrap's worth every ten minutes
# or so. Netherite blocks are deliberately not on the table — four ingots out of
# a single cycle would undo the whole material.
execute store result score #gen.roll ra.temp run random value 1..1000
execute if score #gen.roll ra.temp matches 1..690 run setblock ^ ^ ^1 minecraft:netherrack
execute if score #gen.roll ra.temp matches 691..990 run setblock ^ ^ ^1 minecraft:magma_block
execute if score #gen.roll ra.temp matches 991..1000 run setblock ^ ^ ^1 minecraft:ancient_debris

execute positioned ^ ^ ^1 run particle minecraft:flame ~ ~0.5 ~ 0.3 0.2 0.3 0.01 6
execute positioned ^ ^ ^1 run playsound minecraft:block.netherrack.place block @a[distance=..12] ~ ~ ~ 0.4 1.2
