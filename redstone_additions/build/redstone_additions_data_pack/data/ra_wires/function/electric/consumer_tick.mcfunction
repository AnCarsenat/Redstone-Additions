# /ra_wires:electric/consumer_tick
# Draw this tick's EU off the grid and expose whether it ran.
# Context: as the consumer marker, at its position.
#
# All or nothing: a consumer that cannot afford a full tick's draw takes nothing
# rather than half. Taking a partial amount would let a starved grid drain into
# a row of consumers that all report themselves as off, which is how you get a
# generator running flat out and nothing on the base working.

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.eu_use run data modify entity @s data.properties.eu_use set value 40

execute if data entity @s data.properties{enabled:0b} run data modify entity @s data.status.active set value 0b
execute if data entity @s data.properties{enabled:0b} run return 0

# Guarded: an unguarded read of a missing eu_use writes zero, and a consumer
# that costs nothing runs for free off an empty grid.
function ra_lib:util/property {name:"eu_use",default:40,min:1}
scoreboard players operation #use ra.wires.tmp2 = #prop ra.temp

# Check the grid can cover the whole draw before taking any of it.
function ra_lib:transport/net/read
execute if score #net_amount ra.tr.tmp < #use ra.wires.tmp2 run data modify entity @s data.status.active set value 0b
execute if score #net_amount ra.tr.tmp < #use ra.wires.tmp2 run return 0

execute store result storage ra:wires eu.amount int 1 run scoreboard players get #use ra.wires.tmp2
execute store result score #got ra.wires.tmp run function ra_wires:electric/take_eu with storage ra:wires eu

execute if score #got ra.wires.tmp < #use ra.wires.tmp2 run data modify entity @s data.status.active set value 0b
execute if score #got ra.wires.tmp < #use ra.wires.tmp2 run return 0

data modify entity @s data.status.active set value 1b
particle minecraft:electric_spark ~ ~0.9 ~ 0.25 0.25 0.25 0.01 2
