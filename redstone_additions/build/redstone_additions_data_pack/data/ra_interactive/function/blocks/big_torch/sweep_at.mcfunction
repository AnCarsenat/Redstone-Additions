# /ra_interactive:blocks/big_torch/sweep_at {radius:16,outer:32}
# Internal: the two selectors, once the radius is known.
# Context: as the torch marker, at the torch position.
#
# Split out because a selector's distance cannot be written without a macro, and
# a macro line is re-parsed every time the function runs -- so the reading and
# clamping in `sweep` is kept out of it.

# Count first, because the kill is what makes them stop matching.
$execute store result score #bt.killed ra.temp run execute if entity @e[type=#ra_interactive:spawn_blocked,tag=!ra.big_torch.seen,distance=..$(radius)]

$execute as @e[type=#ra_interactive:spawn_blocked,tag=!ra.big_torch.seen,distance=..$(radius)] at @s run particle minecraft:smoke ~ ~0.5 ~ 0.2 0.3 0.2 0.01 6 normal @a[distance=..24,scores={ra.u.par=1..}]
$kill @e[type=#ra_interactive:spawn_blocked,tag=!ra.big_torch.seen,distance=..$(radius)]

# Remember: everything in the wider band, so a mob walking in is not denied.
$tag @e[type=#ra_interactive:spawn_blocked,distance=..$(outer)] add ra.big_torch.seen

# A running total since the torch was placed, which is what makes the goggles
# line worth reading -- a per-sweep count is almost always 0.
execute unless data entity @s data.data.removed run data modify entity @s data.data.removed set value 0
execute store result score #bt.total ra.temp run data get entity @s data.data.removed 1
scoreboard players operation #bt.total ra.temp += #bt.killed ra.temp
execute store result entity @s data.data.removed int 1 run scoreboard players get #bt.total ra.temp
