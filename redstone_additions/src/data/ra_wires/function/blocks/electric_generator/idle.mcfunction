# /ra_wires:blocks/electric_generator/idle
# Put out a generator that has stopped burning.
# Context: as the generator marker, at its block.
#
# Runs on every tick it is not burning, not just the tick it stops. A light left
# behind by a chunk unload or a marker killed mid-burn would otherwise stay lit
# for ever, and an invisible block with no source is not something a player can
# reasonably find.

execute if entity @s[tag=ra.wires.gen_was_lit] run function ra_wires:blocks/electric_generator/refresh_display
tag @s remove ra.wires.gen_was_lit
tag @s remove ra.wires.gen_lit

execute if block ~ ~1 ~ minecraft:light[level=10] run setblock ~ ~1 ~ air
