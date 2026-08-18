# /ra_infinite:blocks/poppy_generator/single
# Plant one flower somewhere in front of the generator.
# Context: at the block in front of the generator.
#
# The block dead ahead is tried first, then the eight around it, and the first spot
# that takes a flower wins — `plant` sets #poppy.done, and every later cell is
# skipped once it is set.
#
# Insisting on the one block dead ahead is what made this look broken: a generator
# that faces slightly off, or whose soil sits beside rather than in front of it,
# planted nothing and said nothing.

scoreboard players set #poppy.done ra.temp 0

function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~1 ~ ~ run function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~-1 ~ ~ run function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~ ~ ~1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~ ~ ~-1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~1 ~ ~1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~1 ~ ~-1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~-1 ~ ~1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute if score #poppy.done ra.temp matches 0 positioned ~-1 ~ ~-1 run function ra_infinite:blocks/poppy_generator/grow_flower
