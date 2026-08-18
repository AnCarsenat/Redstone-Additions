# /ra_wires:fluid/pump_extract {match,medium,volume,drained,dx,dy,dz}
# Internal: move one world source block into the network, if it fits.

# A liquid pump will not lift a gas, and a gas pump will not lift a liquid.
$execute if entity @s[tag=ra.custom_block.liquid_pump] unless data storage ra:wires media.$(medium){state:"liquid"} run return 0
$execute if entity @s[tag=ra.custom_block.gas_pump] unless data storage ra:wires media.$(medium){state:"gas"} run return 0

# A source block is all or nothing. Offering a partial amount and then removing
# the block anyway is how a nearly full network would quietly delete a lake.
function ra_lib:transport/net/read
$scoreboard players set #need ra.wires.tmp $(volume)
scoreboard players operation #free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #free ra.wires.tmp -= #net_amount ra.tr.tmp

# A network smaller than one source block can never take one, however long you
# wait. That is a different problem from a network that happens to be full, and
# saying so is the difference between "add a tank" and "wait a moment".
execute if score #net_capacity ra.tr.tmp < #need ra.wires.tmp run data modify entity @s data.status.pump_state set value "network_too_small"
execute if score #net_capacity ra.tr.tmp < #need ra.wires.tmp run return 0

execute if score #free ra.wires.tmp < #need ra.wires.tmp run data modify entity @s data.status.pump_state set value "network_full"
execute if score #free ra.wires.tmp < #need ra.wires.tmp run return 0

$execute store result score #accept ra.wires.tmp run function ra_lib:transport/net/offer {amount:$(volume),medium:"$(medium)"}

execute if score #accept ra.wires.tmp matches ..0 run data modify entity @s data.status.pump_state set value "wrong_medium"
execute if score #accept ra.wires.tmp matches ..0 run return 0

# Small pools drain away; a real body of fluid does not.
$execute positioned ~$(dx) ~$(dy) ~$(dz) run function ra_wires:fluid/source/is_infinite {match:"$(match)"}
$execute if score #src_infinite ra.wires.tmp matches 0 positioned ~$(dx) ~$(dy) ~$(dz) run setblock ~ ~ ~ $(drained)

$execute positioned ~$(dx) ~$(dy) ~$(dz) run function ra_wires:fluid/particles {medium:"$(medium)"}
data modify entity @s data.status.pump_state set value "pumping"
