# /ra_wires:blocks/creative_fluid/tick
# Creative Fluid Source: fills its network with a chosen medium, from nothing.
# Context: as its marker, at its block.
#
# The counterpart to the Creative EU Source, and the same reasoning: while you
# are building the consuming half of a fluid system you do not want to also be
# running a pump farm to feed it.
#
# The medium is a property rather than a fixed choice, because a fluid network
# holds exactly one medium at a time -- a source stuck on water could not be used
# to test a lava line at all. Cycle it with the wrench.

execute unless data entity @s data.properties.medium run data modify entity @s data.properties.medium set value "water"

data modify entity @s data.status.medium set from entity @s data.properties.medium

function ra_lib:transport/net/read
scoreboard players operation #cr.fill ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #cr.fill ra.wires.tmp -= #net_amount ra.tr.tmp

execute if score #cr.fill ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value "Network full"

# A network already holding something else refuses this, which is correct and is
# why the medium is worth cycling rather than forcing.
execute store result storage ra:wires cf.amount int 1 run scoreboard players get #cr.fill ra.wires.tmp
data modify storage ra:wires cf.medium set from entity @s data.properties.medium
function ra_wires:blocks/creative_fluid/push with storage ra:wires cf
