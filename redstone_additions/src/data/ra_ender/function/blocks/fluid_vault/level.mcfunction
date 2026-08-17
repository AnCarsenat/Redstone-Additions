# /ra_ender:blocks/fluid_vault/level
# Work out how much a two-way link should move: half the gap, or nothing.
# Context: as the sending vault marker. Reads #ender.mine, writes #ender.want.

# What the partner's network holds. -1 means there is no partner.
scoreboard players set #ender.theirs ra.temp -1
data modify storage ra:ender probe set value {}
data modify storage ra:ender probe.channel set from entity @s data.properties.channel
function ra_ender:link/probe_fluid with storage ra:ender probe

execute if score #ender.theirs ra.temp matches ..-1 run return run scoreboard players set #ender.want ra.temp 0

scoreboard players operation #ender.gap ra.temp = #ender.mine ra.temp
scoreboard players operation #ender.gap ra.temp -= #ender.theirs ra.temp

# Dead zone: below this the pair is level enough, and pushing would just be noise.
execute if score #ender.gap ra.temp matches ..20 run return run scoreboard players set #ender.want ra.temp 0

scoreboard players set #ender.two ra.temp 2
scoreboard players operation #ender.want ra.temp = #ender.gap ra.temp
scoreboard players operation #ender.want ra.temp /= #ender.two ra.temp
