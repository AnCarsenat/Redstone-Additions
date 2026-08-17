# /ra_ender:blocks/fluid_vault/process
# Move a slice of fluid between this network and the partner's.
# Context: as a vault marker, at the block.
#
# Two-way here is easier than for items, because fluid is not made of stacks: a
# pair of linked networks can simply find their level. Only the fuller side ever
# pushes, and it pushes half the difference, so the two converge and neither ever
# hands the same fluid back. A dead zone stops the last few units bouncing.
#
# Take first, then offer, then hand back whatever the far side would not accept.
# In that order the fluid is only ever in one network, and a full receiver or a
# medium mismatch costs nothing.

scoreboard players set @s ra.ender.cd 0

# Held for the length of this call: a two-way vault wears the send and the
# receive tag, so without this every partner search could find itself.
tag @s add ra.ender.self

execute if data entity @s data.properties{enabled:0b} run return run function ra_ender:link/done
execute if data entity @s data.properties{mode:"receive"} run return run function ra_ender:link/done

function ra_lib:transport/net/read
execute if score #net_amount ra.tr.tmp matches ..0 run return run function ra_ender:link/done
execute unless data storage ra:transport cur.medium run return run function ra_ender:link/done

scoreboard players operation #ender.mine ra.temp = #net_amount ra.tr.tmp
execute store result score #ender.rate ra.temp run data get entity @s data.properties.transfer_rate

data modify storage ra:ender fluid set value {}
data modify storage ra:ender fluid.medium set from storage ra:transport cur.medium
data modify storage ra:ender fluid.channel set from entity @s data.properties.channel

# A one-way sender moves up to its rate and does not care what the far side holds.
scoreboard players operation #ender.want ra.temp = #ender.rate ra.temp

# A two-way link moves half the gap instead, and only when it is the fuller side.
execute if data entity @s data.properties{mode:"link"} run function ra_ender:blocks/fluid_vault/level

execute if score #ender.want ra.temp matches ..0 run return run function ra_ender:link/done
execute if score #ender.want ra.temp > #ender.rate ra.temp run scoreboard players operation #ender.want ra.temp = #ender.rate ra.temp
execute store result storage ra:ender fluid.rate int 1 run scoreboard players get #ender.want ra.temp

function ra_ender:link/send_fluid with storage ra:ender fluid

function ra_ender:link/done
