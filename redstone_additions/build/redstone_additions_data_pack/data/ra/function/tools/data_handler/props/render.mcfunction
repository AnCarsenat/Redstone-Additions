# /ra:tools/data_handler/props/render {name,action}
# Internal: one property row, if the selected block has that property. As player.

$execute unless data storage ra:dh properties.$(name) run return 0

# Fields the block declared as its own, in ra:tools/readonly/init_registry. Shown
# with a dead button rather than withheld: a censored row makes a block look like
# it has fewer settings than it does, and leaves the player wondering why a value
# the Goggles happily display is missing here.
#
# No gamemode test. Creative players use the Creative Data Handler, which shows
# and edits everything — so this tool does not need a second mode, and asking
# what gamemode someone is in to decide what a row looks like was complexity
# earning nothing.
$execute if data storage ra:dh hidden.$(name) run scoreboard players add #dh.shown ra.temp 1
$execute if data storage ra:dh hidden.$(name) run return run function ra:tools/data_handler/props/row_locked with storage ra:dh q

scoreboard players add #dh.shown ra.temp 1
function ra:tools/data_handler/props/probe with storage ra:dh q

execute if score #dh.type ra.temp matches 1 run function ra:tools/data_handler/props/row_number with storage ra:dh q
execute if score #dh.type ra.temp matches 2 run function ra:tools/data_handler/props/row_bool with storage ra:dh q
execute if score #dh.type ra.temp matches 3 run function ra:tools/data_handler/props/row_list with storage ra:dh q
execute if score #dh.type ra.temp matches 0 run function ra:tools/data_handler/props/row_string with storage ra:dh q

# The item row carries two buttons, and a macro is substituted from one compound,
# so the second action id is worked out here rather than in the row. 200 + index
# is the same registry index in a second action space -- see props/hand_action.
# On a scratch holder, not on #dh.act: that one is the row iterator and the rest
# of the list is still counting on it.
scoreboard players operation #dh.hand ra.temp = #dh.act ra.temp
scoreboard players add #dh.hand ra.temp 100
execute if score #dh.type ra.temp matches 4 store result storage ra:dh q.hand_action int 1 run scoreboard players get #dh.hand ra.temp
execute if score #dh.type ra.temp matches 4 run function ra:tools/data_handler/props/row_item with storage ra:dh q
