# /ra_infinite:blocks/poppy_generator/check_ground
# Is there anywhere in front worth planting? Sets #poppy.ground ra.temp to 1.
# Context: at the block in front of the generator.
#
# Same 3x3 the planter searches, so the Goggles readout cannot drift from what
# the generator actually does.

function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~1 ~ ~ run function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~-1 ~ ~ run function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~ ~ ~1 run function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~ ~ ~-1 run function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~1 ~ ~1 run function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~1 ~ ~-1 run function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~-1 ~ ~1 run function ra_infinite:blocks/poppy_generator/check_cell
execute positioned ~-1 ~ ~-1 run function ra_infinite:blocks/poppy_generator/check_cell
