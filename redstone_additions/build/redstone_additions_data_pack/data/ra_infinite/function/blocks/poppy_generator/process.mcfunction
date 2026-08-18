# /ra_infinite:blocks/poppy_generator/process
# Plant one flower in front of the generator.
# Context: as the generator's marker, at the generator, rotated with it.

# A missing cooldown used to read as zero here, and zero means "long enough"
# to the comparison below -- the generator then ran every tick.
function ra_lib:util/property {name:"cooldown",default:100,min:1}
execute unless score @s ra.cooldown >= #prop ra.temp run return 0
scoreboard players set @s ra.cooldown 0


# One flower in front, always. The 3x3 patch mode is gone: it was a second code
# path over the same ground for a block whose whole job is one flower at a time.
execute positioned ^ ^ ^1 run function ra_infinite:blocks/poppy_generator/single
