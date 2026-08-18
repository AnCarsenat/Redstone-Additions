# /ra_ender:blocks/power_vault/process
# Move EU from this vault's grid to its partner's, over the channel.
# Context: as a vault marker, at the block.
#
# The vault is a wireless bridge. It belongs to its local grid so wires connect
# to it, but contributes no capacity of its own — it is not a battery, it is a
# link. What it moves comes out of the grid behind it and lands in the grid at
# the far end, which is why two bases can share one set of batteries.
#
# It used to hold a private buffer and trade that buffer with its partner's,
# which meant it was not really wired into anything: charge had to seep into the
# vault under the old per-node model before there was anything to send, and once
# electric moved onto the network engine there was no seeping left to do.
#
# NOTHING IS CREATED OR DESTROYED HERE
# The sender debits its grid only by what the receiver's grid actually accepted,
# and anything the far side could not take is put straight back. A full receiving
# grid costs nothing rather than voiding the difference.

scoreboard players set @s ra.ender.cd 0

# Held for the length of this call: a two-way vault wears the send and the
# receive tag, so without this every partner search could find itself.
tag @s add ra.ender.self

execute if data entity @s data.properties{enabled:0b} run return run function ra_ender:link/done
execute if data entity @s data.properties{mode:"receive"} run return run function ra_ender:link/done

# What this grid can spare.
function ra_lib:transport/net/read
scoreboard players operation #ender.eu ra.temp = #net_amount ra.tr.tmp
function ra_lib:util/property {name:"transfer_rate",default:80,min:1}
scoreboard players operation #ender.rate ra.temp = #prop ra.temp
execute if score #ender.eu ra.temp matches ..0 run return run function ra_ender:link/done

# One-way: up to the rate, whatever the far side holds.
scoreboard players operation #ender.carry ra.temp = #ender.eu ra.temp
execute if score #ender.carry ra.temp > #ender.rate ra.temp run scoreboard players operation #ender.carry ra.temp = #ender.rate ra.temp

# Two-way: half the gap between the two GRIDS, and only downhill.
execute if data entity @s data.properties{mode:"link"} run function ra_ender:blocks/power_vault/level
execute if score #ender.carry ra.temp matches ..0 run return run function ra_ender:link/done

# Take first, so the receiver cannot be handed EU that still exists here. The
# refund below puts back anything it turns out not to want.
execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ender.carry ra.temp
execute store result score #ender.carry ra.temp run function ra_wires:electric/take_eu with storage ra:wires eu
execute if score #ender.carry ra.temp matches ..0 run return run function ra_ender:link/done

data modify storage ra:ender power set value {}
data modify storage ra:ender power.channel set from entity @s data.properties.channel
function ra_ender:link/send_power with storage ra:ender power

# Refund whatever the partner's grid could not hold.
scoreboard players operation #ender.back ra.temp = #ender.carry ra.temp
scoreboard players operation #ender.back ra.temp -= #ender.sent ra.temp
execute if score #ender.back ra.temp matches 1.. run function ra_ender:blocks/power_vault/refund

execute if score #ender.sent ra.temp matches 1.. run particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 3

function ra_ender:link/done
