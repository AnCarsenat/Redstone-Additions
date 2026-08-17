# /ra_infinite:blocks/poppy_generator/check_cell
# One cell of the ground check — the same three heights grow_flower tries.
# Context: at the cell. Sets #poppy.ground ra.temp to 1 if a flower would take.

execute if block ~ ~ ~ #ra_infinite:growable if block ~ ~-1 ~ #ra_infinite:flower_ground run scoreboard players set #poppy.ground ra.temp 1
execute if block ~ ~ ~ #ra_infinite:flower_ground if block ~ ~1 ~ #ra_infinite:growable run scoreboard players set #poppy.ground ra.temp 1
execute if block ~ ~-1 ~ #ra_infinite:growable if block ~ ~-2 ~ #ra_infinite:flower_ground run scoreboard players set #poppy.ground ra.temp 1
