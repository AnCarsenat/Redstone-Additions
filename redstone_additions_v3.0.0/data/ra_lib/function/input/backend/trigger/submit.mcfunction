# /ra_lib:input/backend/trigger/submit
# Capture and validate submitted trigger value.

scoreboard players operation @s ra.input.value = @s ra.input.trigger

# Reset trigger to sentinel and re-enable for future submissions.
scoreboard players set @s ra.input.trigger -2147483648
scoreboard players set @s ra.input.last -2147483648
scoreboard players enable @s ra.input.trigger

execute unless score @s ra.input.value >= @s ra.input.min run tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Value is below minimum.",color:"red"}]
execute unless score @s ra.input.value >= @s ra.input.min run return 0

execute unless score @s ra.input.value <= @s ra.input.max run tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Value is above maximum.",color:"red"}]
execute unless score @s ra.input.value <= @s ra.input.max run return 0

scoreboard players operation @s ra.input.result = @s ra.input.value
scoreboard players set @s ra.input.state 2
tag @s remove ra.input.active

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
execute store result storage ra:input tmp.value int 1 run scoreboard players get @s ra.input.value
function ra_lib:input/backend/trigger/store_choice with storage ra:input tmp
data remove storage ra:input tmp

tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Input captured.",color:"green"}]