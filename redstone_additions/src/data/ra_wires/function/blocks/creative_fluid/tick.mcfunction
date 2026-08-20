# /ra_wires:blocks/creative_fluid/tick
# Creative Fluid Source: fills its network with a chosen medium, from nothing.
# Context: as its marker, at its block.
#
# The counterpart to the Creative EU Source, and the same reasoning: while you
# are building the consuming half of a fluid system you do not want to also be
# running a pump farm to feed it.
#
# The medium is a property rather than a fixed choice: a source stuck on water
# could not be used to test a lava line at all. Cycle it with the wrench, or set
# it on the `medium` row of the Data Handler.
#
# THE WRENCH CYCLES, THE HANDLER TYPES
# The wrench can only ever land on a medium that exists. The Handler takes free
# text, so it can land on one that does not -- and an unrecognised medium pushed
# into a network would open a key nothing can drain, in a run that now holds
# several media and would keep the junk one for ever. So it is checked here, and
# a source set to a name the registry does not know does nothing and says so.

execute unless data entity @s data.properties.medium run data modify entity @s data.properties.medium set value "water"

data modify entity @s data.status.medium set from entity @s data.properties.medium

data modify storage ra:wires cfv set value {}
data modify storage ra:wires cfv.medium set from entity @s data.properties.medium
execute store result score #cr.known ra.wires.tmp run function ra_wires:blocks/creative_fluid/known with storage ra:wires cfv
execute if score #cr.known ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value "Unknown medium"

function ra_lib:transport/net/read
scoreboard players operation #cr.fill ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #cr.fill ra.wires.tmp -= #net_amount ra.tr.tmp

execute if score #cr.fill ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value "Network full"

# A network holding something else takes this alongside it now -- the run clogs on
# the sum rather than on any one medium -- so the fill above is the room left
# across everything in there.
execute store result storage ra:wires cf.amount int 1 run scoreboard players get #cr.fill ra.wires.tmp
data modify storage ra:wires cf.medium set from entity @s data.properties.medium
function ra_wires:blocks/creative_fluid/push with storage ra:wires cf
