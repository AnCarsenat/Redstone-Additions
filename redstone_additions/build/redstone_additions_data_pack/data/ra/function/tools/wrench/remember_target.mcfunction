# /ra:tools/wrench/remember_target
# Internal: store the selected block's position on the player. Context: as the
# player, positioned at the block.

execute store result score @s ra.wr.x run data get entity @e[tag=ra.wrench.sel,limit=1] Pos[0]
execute store result score @s ra.wr.y run data get entity @e[tag=ra.wrench.sel,limit=1] Pos[1]
execute store result score @s ra.wr.z run data get entity @e[tag=ra.wrench.sel,limit=1] Pos[2]
