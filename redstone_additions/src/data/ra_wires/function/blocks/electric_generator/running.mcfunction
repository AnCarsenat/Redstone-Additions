# /ra_wires:blocks/electric_generator/running
# Show that this generator is actually burning.
# Context: as the generator marker, at its block.
#
# Three signals, because a barrel wearing a skin has nothing of its own to show:
# the skin flips to a lit furnace, the block throws smoke and flame the way a
# furnace does, and it lights its own space so a room full of running generators
# is visibly lit rather than pitch dark.

# Repaint only on the tick the state actually changes, so this is not a skin
# respawn every tick for every generator in the world.
execute unless entity @s[tag=ra.wires.gen_lit] run tag @s add ra.wires.gen_lit
execute unless entity @s[tag=ra.wires.gen_was_lit] run function ra_wires:blocks/electric_generator/refresh_display
tag @s add ra.wires.gen_was_lit

particle minecraft:smoke ~ ~1.1 ~ 0.15 0.05 0.15 0.01 2 normal @a[scores={ra.u.par=1..}]
particle minecraft:flame ~ ~0.6 ~ 0.25 0.1 0.25 0.005 1 normal @a[scores={ra.u.par=1..}]

# One block of light above it. Only ever placed into air and only ever removed
# again if it is still the light this put there -- the same discipline the
# Industrial Light uses, so nothing a player built can be replaced.
execute if block ~ ~1 ~ #minecraft:air run setblock ~ ~1 ~ minecraft:light[level=10]
