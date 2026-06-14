# /ra_lib:input/consume
# Consume a ready result and write normalized output to storage ra:input consume.
# Returns 1 when a result was consumed, 0 otherwise.

data remove storage ra:input consume

execute unless score @s ra.input.state matches 2 run return 0

execute store result storage ra:input consume.request int 1 run scoreboard players get @s ra.input.req
execute store result storage ra:input consume.mode int 1 run scoreboard players get @s ra.input.mode
execute store result storage ra:input consume.backend int 1 run scoreboard players get @s ra.input.backend
data modify storage ra:input consume.player set from entity @s UUID

execute if score @s ra.input.mode matches 1 store result storage ra:input consume.choice int 1 run scoreboard players get @s ra.input.result
execute if score @s ra.input.mode matches 3 store result storage ra:input consume.number int 1 run scoreboard players get @s ra.input.result
execute if score @s ra.input.mode matches 2 run function ra_lib:input/session/storage_copy_text_to_consume_prepare

function ra_lib:input/session/cleanup
return 1