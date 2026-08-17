# /ra:tools/data_handler/props/render {name,action}
# Internal: one property row, if the selected block has that property. As player.

$execute unless data storage ra:dh properties.$(name) run return 0

function ra:tools/data_handler/props/probe with storage ra:dh q

execute if score #dh.type ra.temp matches 1 run function ra:tools/data_handler/props/row_number with storage ra:dh q
execute if score #dh.type ra.temp matches 2 run function ra:tools/data_handler/props/row_bool with storage ra:dh q
execute if score #dh.type ra.temp matches 3 run function ra:tools/data_handler/props/row_list with storage ra:dh q
execute if score #dh.type ra.temp matches 0 run function ra:tools/data_handler/props/row_string with storage ra:dh q
