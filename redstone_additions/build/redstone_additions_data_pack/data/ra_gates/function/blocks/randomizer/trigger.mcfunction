# /ra_gates:blocks/randomizer/trigger
# Trigger random output based on chance property

# Chance, read through the guarded reader: a chance stored as the string "50"
# used to read as 2, because data get returns a string's length.
function ra_lib:util/property {name:"chance",default:50,min:0}
scoreboard players operation @s ra.temp = #prop ra.temp

# Clamp chance to 0-100
execute if score @s ra.temp matches ..0 run scoreboard players set @s ra.temp 0
execute if score @s ra.temp matches 101.. run scoreboard players set @s ra.temp 100

# Generate random number 0-99.
#
# This used to be stored in @s ra.power — the redstone library's own output, on
# this same marker, while the caller was still using it. randomizer/process calls
# this in the middle of reading its power: the three lines after the call decide
# the rising-edge latch and whether to clear the output, and they were reading a
# dice roll instead of a power level. One roll in a hundred came up 0 and made a
# Randomizer wipe its own output the instant after it fired, and the latch that
# stops it re-firing while held was decided by chance rather than by redstone.
#
# Scratch belongs on a fake player. #rng is not a marker, so nothing else can be
# reading it, and ra.power now means what the library set it to.
execute store result score #rng ra.temp run random value 0..99

# Output: if random < chance, output ON
execute if score #rng ra.temp < @s ra.temp at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute unless score #rng ra.temp < @s ra.temp at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# Particle feedback
execute if score #rng ra.temp < @s ra.temp run particle minecraft:happy_villager ~ ~1 ~ 0.3 0.3 0.3 0.02 5
execute unless score #rng ra.temp < @s ra.temp run particle minecraft:smoke ~ ~1 ~ 0.3 0.3 0.3 0.02 5
