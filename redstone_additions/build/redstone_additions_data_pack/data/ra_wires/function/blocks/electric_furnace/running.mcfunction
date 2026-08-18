# /ra_wires:blocks/electric_furnace/running
# Show that this furnace just did work. Context: as the marker, at the block.
#
# Repainted only on the tick the state changes, so this is not a skin respawn
# every tick for every furnace in the world.

execute unless entity @s[tag=ra.wires.ef_lit] run tag @s add ra.wires.ef_lit
execute unless entity @s[tag=ra.wires.ef_was_lit] run function ra_wires:blocks/electric_furnace/refresh_display
tag @s add ra.wires.ef_was_lit

data modify entity @s data.status.active set value 1b
particle minecraft:smoke ~ ~1.1 ~ 0.15 0.05 0.15 0.01 2
particle minecraft:flame ~ ~0.6 ~ 0.2 0.1 0.2 0.005 1
playsound minecraft:block.blastfurnace.fire_crackle block @a[distance=..10] ~ ~ ~ 0.3 1.6

# A lit furnace lights the room in vanilla; a skin is only a picture and emits
# nothing, so the light is placed explicitly. Only into air, and idle removes only
# the exact state placed here, so nothing a player built can be replaced.
execute if block ~ ~1 ~ #minecraft:air run setblock ~ ~1 ~ minecraft:light[level=10]
