# /ra:tools/data_handler/generic_action
# Handle a property row's button. As player, with ra.dh.action at 100 + row index.
#
# The action id carries the registry index rather than a per-property constant,
# which is what lets one handler serve every property.

scoreboard players operation #dh.idx ra.temp = @s ra.dh.action
scoreboard players remove #dh.idx ra.temp 100

data modify storage ra:dh q set value {}
execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.idx ra.temp
function ra:tools/data_handler/props/pick_name with storage ra:dh q
execute unless data storage ra:dh pending_name run return 0
data modify storage ra:dh q.name set from storage ra:dh pending_name

# A dead button stops the click, but the row's action is a /trigger and a player
# can still type one. This is the refusal that actually holds.
function ra:tools/data_handler/collect_hidden
function ra:tools/data_handler/props/deny with storage ra:dh q
execute if data storage ra:dh denied run return run data remove storage ra:dh denied

function ra:tools/data_handler/props/probe with storage ra:dh q

# A flag needs no input: flip it and show the result.
execute if score #dh.type ra.temp matches 2 run function ra:tools/data_handler/props/toggle with storage ra:dh q
execute if score #dh.type ra.temp matches 2 run function ra:tools/data_handler/refresh
execute if score #dh.type ra.temp matches 2 run return 0

# Everything else waits for the input form. The kind is remembered so the apply
# step does not have to probe a value that is about to be replaced.
scoreboard players operation @s ra.dh.pending = @s ra.dh.action

data modify storage ra:dh pending_kind set value "text"
execute if score #dh.type ra.temp matches 1 run data modify storage ra:dh pending_kind set value "number"
execute if score #dh.type ra.temp matches 3 run data modify storage ra:dh pending_kind set value "list"

execute if score #dh.type ra.temp matches 1 run function ra:tools/data_handler/props/ask_number with storage ra:dh q
execute unless score #dh.type ra.temp matches 1 run function ra:tools/data_handler/props/ask_text with storage ra:dh q
