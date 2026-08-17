# /ra_ender:blocks/power_vault/process
# Move EU between this vault's buffer and its partner's.
# Context: as a vault marker, at the block.
#
# Same shape as the fluid link: in two-way mode only the fuller buffer pushes, and
# it pushes half the gap, so the pair settles instead of trading the same charge
# back and forth. The amount is worked out from both buffers before anything is
# written, and the receiver reports what it actually took, so the sum across the
# pair is the same before and after.

scoreboard players set @s ra.ender.cd 0

# Held for the length of this call: a two-way vault wears the send and the
# receive tag, so without this every partner search could find itself.
tag @s add ra.ender.self

execute if data entity @s data.properties{enabled:0b} run return run function ra_ender:link/done
execute if data entity @s data.properties{mode:"receive"} run return run function ra_ender:link/done

execute store result score #ender.eu ra.temp run data get entity @s data.data.eu
execute store result score #ender.rate ra.temp run data get entity @s data.properties.transfer_rate
execute if score #ender.eu ra.temp matches ..0 run return run function ra_ender:link/done

# One-way: up to the rate, whatever the far side holds.
scoreboard players operation #ender.carry ra.temp = #ender.eu ra.temp
execute if score #ender.carry ra.temp > #ender.rate ra.temp run scoreboard players operation #ender.carry ra.temp = #ender.rate ra.temp

# Two-way: half the gap, and only downhill.
execute if data entity @s data.properties{mode:"link"} run function ra_ender:blocks/power_vault/level
execute if score #ender.carry ra.temp matches ..0 run return run function ra_ender:link/done

data modify storage ra:ender power set value {}
data modify storage ra:ender power.channel set from entity @s data.properties.channel
function ra_ender:link/send_power with storage ra:ender power

# Whatever the partner took, this side loses — and only that.
execute if score #ender.sent ra.temp matches 1.. run scoreboard players operation #ender.eu ra.temp -= #ender.sent ra.temp
execute if score #ender.sent ra.temp matches 1.. store result entity @s data.data.eu int 1 run scoreboard players get #ender.eu ra.temp
execute if score #ender.sent ra.temp matches 1.. run particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 3

function ra_ender:link/done
