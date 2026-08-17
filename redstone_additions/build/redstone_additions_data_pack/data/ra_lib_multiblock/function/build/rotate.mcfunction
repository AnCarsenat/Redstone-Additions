# /ra_lib_multiblock:build/rotate
# Internal: rotate (#mb_x,#mb_y,#mb_z) about the vertical axis into
# (#mb_rx,#mb_ry,#mb_rz), for the facing selected by #mb_rot.
#
# Offsets are authored for a north-facing structure, where "in front of the base"
# is -Z. The three other facings are the quarter turns of that:
#   north (0)  ( x, y,  z)
#   south (1)  (-x, y, -z)
#   east  (2)  (-z, y,  x)
#   west  (3)  ( z, y, -x)
# Height never changes, so ry is always y.

scoreboard players operation #mb_ry ra.multiblock = #mb_y ra.multiblock

execute if score #mb_rot ra.multiblock matches 0 run scoreboard players operation #mb_rx ra.multiblock = #mb_x ra.multiblock
execute if score #mb_rot ra.multiblock matches 0 run scoreboard players operation #mb_rz ra.multiblock = #mb_z ra.multiblock

execute if score #mb_rot ra.multiblock matches 1 run scoreboard players operation #mb_rx ra.multiblock = #mb_x ra.multiblock
execute if score #mb_rot ra.multiblock matches 1 run scoreboard players operation #mb_rx ra.multiblock *= #mb_neg ra.multiblock
execute if score #mb_rot ra.multiblock matches 1 run scoreboard players operation #mb_rz ra.multiblock = #mb_z ra.multiblock
execute if score #mb_rot ra.multiblock matches 1 run scoreboard players operation #mb_rz ra.multiblock *= #mb_neg ra.multiblock

execute if score #mb_rot ra.multiblock matches 2 run scoreboard players operation #mb_rx ra.multiblock = #mb_z ra.multiblock
execute if score #mb_rot ra.multiblock matches 2 run scoreboard players operation #mb_rx ra.multiblock *= #mb_neg ra.multiblock
execute if score #mb_rot ra.multiblock matches 2 run scoreboard players operation #mb_rz ra.multiblock = #mb_x ra.multiblock

execute if score #mb_rot ra.multiblock matches 3 run scoreboard players operation #mb_rx ra.multiblock = #mb_z ra.multiblock
execute if score #mb_rot ra.multiblock matches 3 run scoreboard players operation #mb_rz ra.multiblock = #mb_x ra.multiblock
execute if score #mb_rot ra.multiblock matches 3 run scoreboard players operation #mb_rz ra.multiblock *= #mb_neg ra.multiblock
