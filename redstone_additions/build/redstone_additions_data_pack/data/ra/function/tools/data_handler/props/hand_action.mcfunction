# /ra:tools/data_handler/props/hand_action
# Handle a [Set from hand] button. As player, with ra.dh.action at 200 + index.
#
# The mirror of generic_action for the second action space. Same index lookup,
# same refusal for a field the block keeps to itself, and then a straight write
# rather than an input form -- there is nothing to ask for, the answer is in the
# player's hand.

scoreboard players operation #dh.idx ra.temp = @s ra.dh.action
scoreboard players remove #dh.idx ra.temp 200

data modify storage ra:dh q set value {}
execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.idx ra.temp
function ra:tools/data_handler/props/pick_name with storage ra:dh q
execute unless data storage ra:dh pending_name run return 0
data modify storage ra:dh q.name set from storage ra:dh pending_name

function ra:tools/data_handler/collect_hidden
function ra:tools/data_handler/props/deny with storage ra:dh q
execute if data storage ra:dh denied run return run data remove storage ra:dh denied

function ra:tools/data_handler/props/set_from_hand with storage ra:dh q
function ra:tools/data_handler/refresh
