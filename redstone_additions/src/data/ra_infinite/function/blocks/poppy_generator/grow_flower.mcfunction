# /ra_infinite:blocks/poppy_generator/grow_flower
# Plant one flower at, above or below the current position.
# Context: at the target position — the block in front of the generator.
#
# A flower only survives on `#ra_infinite:flower_ground` — the vanilla dirt family
# plus farmland — and providing that is the player's job. The generator plants; it
# does not terraform.
#
# Three spots are tried, because all three are what "in front of it" means in
# practice, and which one applies depends only on how the generator was placed:
#
#   1. the block in front, ground one below it — generator level with the soil;
#   2. one block up, when the block in front *is* the soil — a generator aimed
#      straight at a grass block, which is how most people set this up;
#   3. one block down, ground two below — a generator on a pedestal, planting on
#      the ground it overlooks.
#
# Not tried: anything further away, and anything at all when the generator faces
# up, since the only thing under that target is the generator itself.

execute if block ~ ~ ~ #ra_infinite:growable if block ~ ~-1 ~ #ra_infinite:flower_ground run return run function ra_infinite:blocks/poppy_generator/plant
execute if block ~ ~ ~ #ra_infinite:flower_ground if block ~ ~1 ~ #ra_infinite:growable positioned ~ ~1 ~ run return run function ra_infinite:blocks/poppy_generator/plant
execute if block ~ ~-1 ~ #ra_infinite:growable if block ~ ~-2 ~ #ra_infinite:flower_ground positioned ~ ~-1 ~ run function ra_infinite:blocks/poppy_generator/plant
