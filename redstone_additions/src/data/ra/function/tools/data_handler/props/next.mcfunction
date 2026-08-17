# /ra:tools/data_handler/props/next
# Internal: render one registry entry, then the rest. As player.

execute unless data storage ra:dh iter[0] run return 0

data modify storage ra:dh q set value {}
data modify storage ra:dh q.name set from storage ra:dh iter[0]
execute store result storage ra:dh q.action int 1 run scoreboard players get #dh.act ra.temp
function ra:tools/data_handler/props/render with storage ra:dh q

data remove storage ra:dh iter[0]
scoreboard players add #dh.act ra.temp 1
function ra:tools/data_handler/props/next
