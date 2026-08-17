# /ra_lib_multiblock:build/entry_next
# Internal: rotate one entry off the work queue and write it into the
# destination selected by work.kind, then recurse.

execute unless data storage ra:multiblock work_q[0] run return 0

data modify storage ra:multiblock work_cur set from storage ra:multiblock work_q[0]
data remove storage ra:multiblock work_q[0]

execute store result score #mb_x ra.multiblock run data get storage ra:multiblock work_cur.x
execute store result score #mb_y ra.multiblock run data get storage ra:multiblock work_cur.y
execute store result score #mb_z ra.multiblock run data get storage ra:multiblock work_cur.z
function ra_lib_multiblock:build/rotate

execute store result storage ra:multiblock work_cur.x int 1 run scoreboard players get #mb_rx ra.multiblock
execute store result storage ra:multiblock work_cur.y int 1 run scoreboard players get #mb_ry ra.multiblock
execute store result storage ra:multiblock work_cur.z int 1 run scoreboard players get #mb_rz ra.multiblock

# The writer is a macro, so the type and facing travel with the entry.
data modify storage ra:multiblock work_cur.type set from storage ra:multiblock work.type
data modify storage ra:multiblock work_cur.facing set from storage ra:multiblock work.facing

execute if data storage ra:multiblock work{kind:"blocks"} run function ra_lib_multiblock:build/write_block with storage ra:multiblock work_cur
execute unless data storage ra:multiblock work{kind:"blocks"} run function ra_lib_multiblock:build/write_io with storage ra:multiblock work_cur

function ra_lib_multiblock:build/entry_next
