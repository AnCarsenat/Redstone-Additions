# /ra_wires:electric/update_status
# Publish the grid's charge, and which way it is going, on this node.
# Context: as an electric node marker.
#
# The figures are the whole network's, because that is where the charge lives.
# Every node on a grid reports the same numbers — the honest answer, since a wire
# does not hold its own pocket of electricity.
#
# WHY A TREND AND NOT AN `enabled` FLAG
# Wires used to show "Enabled: on", which was true of every wire that had ever
# been placed and told you nothing. What you actually want to know standing in
# front of a wire is whether the grid is filling or draining, and that is one
# subtraction against what the same node read a second ago.

function ra_lib:transport/net/read

execute store result entity @s data.status.available_eu int 1 run scoreboard players get #net_amount ra.tr.tmp
execute store result entity @s data.status.capacity int 1 run scoreboard players get #net_capacity ra.tr.tmp

# Trend against this node's own last reading.
execute unless data entity @s data.data.last_eu run data modify entity @s data.data.last_eu set value 0
execute store result score #eu.prev ra.wires.tmp run data get entity @s data.data.last_eu 1
scoreboard players operation #eu.delta ra.wires.tmp = #net_amount ra.tr.tmp
scoreboard players operation #eu.delta ra.wires.tmp -= #eu.prev ra.wires.tmp
execute store result entity @s data.data.last_eu int 1 run scoreboard players get #net_amount ra.tr.tmp
execute store result entity @s data.status.trend int 1 run scoreboard players get #eu.delta ra.wires.tmp

# One line that says what the grid is doing, in the order that matters: not being
# on a grid beats being full, and being full beats which way the needle moved.
execute if score #eu.delta ra.wires.tmp matches 1.. run data modify entity @s data.status.grid set value "Charging"
execute if score #eu.delta ra.wires.tmp matches ..-1 run data modify entity @s data.status.grid set value "Draining"
execute if score #eu.delta ra.wires.tmp matches 0 run data modify entity @s data.status.grid set value "Steady"

execute if score #net_amount ra.tr.tmp matches 1.. if score #net_amount ra.tr.tmp >= #net_capacity ra.tr.tmp run data modify entity @s data.status.grid set value "Grid full"
execute if score #net_amount ra.tr.tmp matches ..0 run data modify entity @s data.status.grid set value "Empty"

# No capacity anywhere on the run: wires and switches store nothing, so a grid
# with no battery on it cannot hold a thing between ticks. Worth saying outright,
# because it looks identical to an empty grid otherwise.
execute if score #net_capacity ra.tr.tmp matches 0 run data modify entity @s data.status.grid set value "No storage — add a Battery"

# A node on no grid at all: a switch someone turned off, or a wire with nothing
# adjacent. That beats every other reading.
execute if score @s ra.tr.net matches ..0 run data modify entity @s data.status.grid set value "Disconnected"
