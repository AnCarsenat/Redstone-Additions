# /ra_wires:blocks/industrial_light/space
# Internal: one space of the beam, then carry on down it.
# Context: at the space.
#
# Lighting stops at the first thing that is not air: a beam should not shine
# through a wall. Unlighting does not stop, because the wall may have been built
# after the beam was lit and the lights beyond it still have to come back out.
#
# `minecraft:air` and `minecraft:cave_air` by name rather than the `#minecraft:air`
# tag. Underground, every empty space is cave_air, so a tag that covers both is
# the tidier test -- but the tag is one more thing that has to be right in a chain
# that was silently doing nothing, and naming the two blocks costs one command.
#
# Removal is still pinned to level=15, the exact state this places. Nothing a
# player built, including a light block of any other level, can be removed here.

scoreboard players set #il.ok ra.wires.tmp2 0
execute if block ~ ~ ~ minecraft:air run scoreboard players set #il.ok ra.wires.tmp2 1
execute if block ~ ~ ~ minecraft:cave_air run scoreboard players set #il.ok ra.wires.tmp2 1
execute if block ~ ~ ~ minecraft:light[level=15] run scoreboard players set #il.ok ra.wires.tmp2 1

execute if score #il.mode ra.wires.tmp2 matches 1 if score #il.ok ra.wires.tmp2 matches 1 run setblock ~ ~ ~ minecraft:light[level=15]
execute if score #il.mode ra.wires.tmp2 matches 1 if score #il.ok ra.wires.tmp2 matches 0 run return 0

execute if score #il.mode ra.wires.tmp2 matches 0 if block ~ ~ ~ minecraft:light[level=15] run setblock ~ ~ ~ minecraft:air

function ra_wires:blocks/industrial_light/step with storage ra:wires beam
