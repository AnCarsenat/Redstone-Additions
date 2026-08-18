# /ra_wires:blocks/industrial_light/space {dx,dy,dz,mode}
# Internal: one space of the beam, then carry on down it.
# Context: at the space.
#
# Lighting stops at the first thing that is not air: a beam should not shine
# through a wall. Unlighting does not stop, because the wall may have been built
# after the beam was lit and the lights beyond it still have to come back out.

$execute if data storage ra:wires beam{mode:"on"} if block ~ ~ ~ #minecraft:air run setblock ~ ~ ~ minecraft:light[level=15]
$execute if data storage ra:wires beam{mode:"on"} unless block ~ ~ ~ minecraft:light[level=15] run return 0

# Only ever a light block, and only ever the exact state this places. Nothing a
# player built can be removed by this, whatever is standing in the beam.
$execute if data storage ra:wires beam{mode:"off"} if block ~ ~ ~ minecraft:light[level=15] run setblock ~ ~ ~ air

$function ra_wires:blocks/industrial_light/step with storage ra:wires beam
