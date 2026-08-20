# /ra_wires:blocks/electric_furnace/stay_lit
# Put the furnace into its working look and keep it there.
# Context: as the marker, at the block.
#
# The look, and nothing else -- no particles, no sound. Those belong to the tick
# an item actually finishes, not to every tick of the cook, so `running` adds
# them on top of this and `cooking` does not.
#
# Repainted only on the tick the state changes, so this is not a skin rebuild
# every tick for every furnace in the world.

execute unless entity @s[tag=ra.wires.ef_lit] run tag @s add ra.wires.ef_lit
execute unless entity @s[tag=ra.wires.ef_was_lit] run function ra_wires:blocks/electric_furnace/refresh_display
tag @s add ra.wires.ef_was_lit

data modify entity @s data.status.active set value 1b

# A lit furnace lights the room in vanilla; a skin is only a picture and emits
# nothing, so the light is placed explicitly. Only into air, and idle removes only
# the exact state placed here, so nothing a player built can be replaced.
execute if block ~ ~1 ~ #minecraft:air run setblock ~ ~1 ~ minecraft:light[level=10]
