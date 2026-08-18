# /ra_wires:blocks/creative_eu/tick
# Creative EU Source: fills its grid from nothing, every tick.
# Context: as its marker, at its block.
#
# A testing and building block. It has no fuel, no recipe and no cost -- the
# point is to take the power supply out of the question entirely while you are
# working on the half of the build that consumes it.
#
# It fills to capacity rather than offering a fixed rate, so it does not matter
# how much the grid draws: whatever was spent last tick is back this tick, and a
# machine on a creative grid never sees a brownout. net/offer clamps to the free
# space itself, so asking for the whole capacity is safe and needs no arithmetic
# here.


function ra_lib:transport/net/read
scoreboard players operation #cr.fill ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #cr.fill ra.wires.tmp -= #net_amount ra.tr.tmp

execute if score #cr.fill ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value "Grid full"

execute store result storage ra:wires eu.amount int 1 run scoreboard players get #cr.fill ra.wires.tmp
execute store result score #cr.made ra.wires.tmp run function ra_wires:electric/offer_eu with storage ra:wires eu

data modify entity @s data.status.state set value "Generating"
execute store result entity @s data.status.filled int 1 run scoreboard players get #cr.made ra.wires.tmp

particle minecraft:end_rod ~ ~0.8 ~ 0.25 0.15 0.25 0.01 3
