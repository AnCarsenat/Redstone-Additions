# /ra:tools/wrench/menu_row_next
# Internal: draw one menu row, then the next. Context: as the marker.

execute unless data storage ra:wrench list[0] run return 0

data modify storage ra:wrench row set from storage ra:wrench list[0]
# The button carries index+1. A trigger score of 0 is indistinguishable from
# "nobody clicked anything", and row 0 is a real row -- so every action id is
# shifted by one and menu_action shifts it back.
scoreboard players operation #wr.btn ra.temp = #wr.i ra.temp
scoreboard players add #wr.btn ra.temp 1
execute store result storage ra:wrench row.i int 1 run scoreboard players get #wr.btn ra.temp
function ra:tools/wrench/menu_row with storage ra:wrench row

data remove storage ra:wrench list[0]
scoreboard players add #wr.i ra.temp 1
function ra:tools/wrench/menu_row_next
