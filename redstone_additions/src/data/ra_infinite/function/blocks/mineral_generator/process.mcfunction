# /ra_infinite:blocks/mineral_generator/process
# Grow one block of ore in front of the generator.
# Context: as the generator's marker, at the generator, rotated with it.

# Wait out the configured period.
# A missing cooldown used to read as zero here, and zero means "long enough"
# to the comparison below -- the generator then ran every tick.
function ra_lib:util/property {name:"cooldown",default:100,min:1}
execute unless score @s ra.cooldown >= #prop ra.temp run return 0
scoreboard players set @s ra.cooldown 0


# Somebody has to take the last one away first.
execute unless block ^ ^ ^1 #ra_infinite:growable run return 0

# Per mille, so the rare end of the table can be tuned finely. At the default
# 100-tick period that is a diamond roughly every seven minutes and an emerald
# roughly every half hour — a generator is a trickle, not a quarry.
execute store result score #gen.roll ra.temp run random value 1..1000
execute if score #gen.roll ra.temp matches 1..570 run setblock ^ ^ ^1 minecraft:stone
execute if score #gen.roll ra.temp matches 571..770 run setblock ^ ^ ^1 minecraft:coal_ore
execute if score #gen.roll ra.temp matches 771..910 run setblock ^ ^ ^1 minecraft:iron_ore
execute if score #gen.roll ra.temp matches 911..985 run setblock ^ ^ ^1 minecraft:redstone_ore
execute if score #gen.roll ra.temp matches 986..997 run setblock ^ ^ ^1 minecraft:diamond_ore
execute if score #gen.roll ra.temp matches 998..1000 run setblock ^ ^ ^1 minecraft:emerald_ore

execute positioned ^ ^ ^1 run particle minecraft:crit ~ ~0.5 ~ 0.3 0.2 0.3 0.05 6
execute positioned ^ ^ ^1 run playsound minecraft:block.stone.place block @a[distance=..12] ~ ~ ~ 0.4 1.4
