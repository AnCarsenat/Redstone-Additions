# /ra_ender:blocks/power_vault/level
# Half the gap between the two buffers, or nothing.
# Context: as the sending vault marker. Reads #ender.eu, writes #ender.carry.

scoreboard players set #ender.theirs ra.temp -1
data modify storage ra:ender probe set value {}
data modify storage ra:ender probe.channel set from entity @s data.properties.channel
function ra_ender:link/probe_power with storage ra:ender probe

execute if score #ender.theirs ra.temp matches ..-1 run return run scoreboard players set #ender.carry ra.temp 0

scoreboard players operation #ender.gap ra.temp = #ender.eu ra.temp
scoreboard players operation #ender.gap ra.temp -= #ender.theirs ra.temp
execute if score #ender.gap ra.temp matches ..4 run return run scoreboard players set #ender.carry ra.temp 0

scoreboard players set #ender.two ra.temp 2
scoreboard players operation #ender.carry ra.temp = #ender.gap ra.temp
scoreboard players operation #ender.carry ra.temp /= #ender.two ra.temp
execute if score #ender.carry ra.temp > #ender.rate ra.temp run scoreboard players operation #ender.carry ra.temp = #ender.rate ra.temp
