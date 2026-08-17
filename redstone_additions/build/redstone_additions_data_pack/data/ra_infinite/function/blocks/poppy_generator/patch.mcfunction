# /ra_infinite:blocks/poppy_generator/patch
# Plant the 3×3 around the target position.
# Context: at the block in front of the generator.
#
# Each cell rolls its own flower, so a patch comes out mixed rather than nine
# copies of one flower.

execute positioned ~-1 ~ ~-1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute positioned ~ ~ ~-1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute positioned ~1 ~ ~-1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute positioned ~-1 ~ ~ run function ra_infinite:blocks/poppy_generator/grow_flower
function ra_infinite:blocks/poppy_generator/grow_flower
execute positioned ~1 ~ ~ run function ra_infinite:blocks/poppy_generator/grow_flower
execute positioned ~-1 ~ ~1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute positioned ~ ~ ~1 run function ra_infinite:blocks/poppy_generator/grow_flower
execute positioned ~1 ~ ~1 run function ra_infinite:blocks/poppy_generator/grow_flower
