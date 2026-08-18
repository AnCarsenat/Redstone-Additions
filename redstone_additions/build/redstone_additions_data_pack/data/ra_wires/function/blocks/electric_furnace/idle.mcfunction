# /ra_wires:blocks/electric_furnace/idle
# Not working this tick. Context: as the marker, at the block.
#
# The repaint is conditional on having BEEN lit, so a furnace sitting idle costs
# one tag test rather than a skin rebuild every tick.

execute if entity @s[tag=ra.wires.ef_was_lit] run tag @s remove ra.wires.ef_lit
execute if entity @s[tag=ra.wires.ef_was_lit] run function ra_wires:blocks/electric_furnace/refresh_display
tag @s remove ra.wires.ef_was_lit
data modify entity @s data.status.active set value 0b
execute if block ~ ~1 ~ minecraft:light[level=10] run setblock ~ ~1 ~ minecraft:air
