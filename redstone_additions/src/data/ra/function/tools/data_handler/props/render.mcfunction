# /ra:tools/data_handler/props/render {name,action}
# Internal: one property row, if the selected block has that property. As player.

$execute unless data storage ra:dh properties.$(name) run return 0

# Tuning fields stay out of survival hands. Creative mode is the test: it is what
# the Creative Data Handler assumes, and a data pack cannot read permission level.
$execute unless entity @s[gamemode=creative] if data storage ra:dh creative_only.$(name) run scoreboard players add #dh.hidden ra.temp 1
$execute unless entity @s[gamemode=creative] if data storage ra:dh creative_only.$(name) run return 0

function ra:tools/data_handler/props/probe with storage ra:dh q

execute if score #dh.type ra.temp matches 1 run function ra:tools/data_handler/props/row_number with storage ra:dh q
execute if score #dh.type ra.temp matches 2 run function ra:tools/data_handler/props/row_bool with storage ra:dh q
execute if score #dh.type ra.temp matches 3 run function ra:tools/data_handler/props/row_list with storage ra:dh q
execute if score #dh.type ra.temp matches 0 run function ra:tools/data_handler/props/row_string with storage ra:dh q
