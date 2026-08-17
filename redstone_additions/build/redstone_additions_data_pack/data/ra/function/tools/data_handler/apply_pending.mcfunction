# /ra:tools/data_handler/apply_pending
# Apply ready input values to the currently pending property edit.

execute unless entity @e[type=marker,tag=ra.dh_target,limit=1] run scoreboard players set @s ra.dh.pending 0
execute unless entity @e[type=marker,tag=ra.dh_target,limit=1] run return 0

# A cancelled session (the player dropped the form) never becomes ready, so stop
# waiting for it. `submit` clears ra.input.active but leaves state at 2, hence
# the second condition — without it a finished input would be discarded here on
# the very tick it became available.
execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run scoreboard players set @s ra.dh.pending 0
execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run return 0

execute store result score @s ra.temp run function ra_lib:input/poll
execute unless score @s ra.temp matches 2 run return 0

execute store result score @s ra.temp run function ra_lib:input/consume
execute unless score @s ra.temp matches 1 run return 0

# One write for every property: the pending id is 100 + the registry index, and
# storage ra:dh pending_name / pending_kind carry the rest.
execute if score @s ra.dh.pending matches 100.. run function ra:tools/data_handler/generic_apply

scoreboard players set @s ra.dh.pending 0
tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Property updated.",color:"green"}]
function ra:tools/data_handler/refresh
