# /ra_lib:input/init
# Initialize modular input scoreboards and storage.

scoreboard objectives add ra.input.state dummy
scoreboard objectives add ra.input.backend dummy
scoreboard objectives add ra.input.mode dummy
scoreboard objectives add ra.input.min dummy
scoreboard objectives add ra.input.max dummy
scoreboard objectives add ra.input.result dummy
scoreboard objectives add ra.input.ttl dummy
scoreboard objectives add ra.input.req dummy
scoreboard objectives add ra.input.next dummy
scoreboard objectives add ra.input.last dummy
scoreboard objectives add ra.input.value dummy
scoreboard objectives add ra.input.trigger trigger

scoreboard players add #next ra.input.next 0
execute unless score #next ra.input.next matches 1.. run scoreboard players set #next ra.input.next 1

data merge storage ra:input {sessions:{},consume:{}}