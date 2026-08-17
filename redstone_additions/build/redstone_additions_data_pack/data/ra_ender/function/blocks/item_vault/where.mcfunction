# /ra_ender:blocks/item_vault/where
# Write this barrel's block coordinates into storage ra:ender move.
# Context: as the vault marker, at the barrel.
#
# `data get Pos[0] 1` truncates toward zero, so at x = -10.5 it reports -10 while
# the block is -11 — and that block is the one whose slot gets cleared after the
# copy. Reading thousandths and dividing gets it right at negative coordinates
# too, because scoreboard division rounds down.

scoreboard players set #ender.k ra.temp 1000
execute store result score #ender.x ra.temp run data get entity @s Pos[0] 1000
execute store result score #ender.y ra.temp run data get entity @s Pos[1] 1000
execute store result score #ender.z ra.temp run data get entity @s Pos[2] 1000
scoreboard players operation #ender.x ra.temp /= #ender.k ra.temp
scoreboard players operation #ender.y ra.temp /= #ender.k ra.temp
scoreboard players operation #ender.z ra.temp /= #ender.k ra.temp

data modify storage ra:ender move set value {}
execute store result storage ra:ender move.x int 1 run scoreboard players get #ender.x ra.temp
execute store result storage ra:ender move.y int 1 run scoreboard players get #ender.y ra.temp
execute store result storage ra:ender move.z int 1 run scoreboard players get #ender.z ra.temp
