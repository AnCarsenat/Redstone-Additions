# /ra_lib:input/session/create
# Create a new input session for the executing player.

tag @s add ra.input.active

scoreboard players set @s ra.input.state 1
scoreboard players set @s ra.input.result -1
scoreboard players set @s ra.input.backend 0
scoreboard players set @s ra.input.ttl 600
scoreboard players set @s ra.input.value 0

# Use an int-min sentinel to detect changes from /trigger, including submissions of 0.
scoreboard players set @s ra.input.trigger -2147483648
scoreboard players set @s ra.input.last -2147483648
scoreboard players enable @s ra.input.trigger

scoreboard players operation @s ra.input.req = #next ra.input.next
scoreboard players add #next ra.input.next 1

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
execute store result storage ra:input tmp.mode int 1 run scoreboard players get @s ra.input.mode
function ra_lib:input/session/storage_create with storage ra:input tmp
data remove storage ra:input tmp