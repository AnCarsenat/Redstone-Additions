# /ra_lib:input/session/cleanup
# Clean all state for the executing player's input session.

tag @s remove ra.input.active

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
execute if score @s ra.input.backend matches 2 run function ra_lib:input/backend/writable_book/restore_current_hand
function ra_lib:input/session/storage_remove with storage ra:input tmp
data remove storage ra:input tmp

scoreboard players set @s ra.input.state 0
scoreboard players set @s ra.input.backend 0
scoreboard players set @s ra.input.mode 0
scoreboard players set @s ra.input.min 0
scoreboard players set @s ra.input.max 0
scoreboard players set @s ra.input.result -1
scoreboard players set @s ra.input.ttl 0
scoreboard players set @s ra.input.req 0
scoreboard players set @s ra.input.last -2147483648
scoreboard players set @s ra.input.value 0
scoreboard players set @s ra.input.trigger -2147483648