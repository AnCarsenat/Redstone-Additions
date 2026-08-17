# /ra_ender:tools/anchor/copy_row
# Internal: copy typed rows into the anchor's table, one per call.
# Context: as a player. storage ra:ender table = {rows:[...],i:N}.

execute unless data storage ra:ender table.rows[0] run return 0
execute if data storage ra:ender table{i:15} run return 0

function ra_ender:tools/anchor/copy_row_write with storage ra:ender table

data remove storage ra:ender table.rows[0]
execute store result score #ender.i ra.temp run data get storage ra:ender table.i
scoreboard players add #ender.i ra.temp 1
execute store result storage ra:ender table.i int 1 run scoreboard players get #ender.i ra.temp

function ra_ender:tools/anchor/copy_row
