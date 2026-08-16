# /ra_wires:fluid/pump_tick
# Pull a world fluid source into the network.
# Context: as a pump marker, at the pump position.
#
# Runs on a duty cycle rather than every tick: a source block is a whole 1000
# units, so pulling one per tick per pump both trivialised supply and ran the
# adjacency scan sixty times a second for no reason.

execute unless data entity @s data.properties{enabled:1b} run return 0

scoreboard players add @s ra.cooldown 1
execute unless score @s ra.cooldown matches 20.. run return 0
scoreboard players set @s ra.cooldown 0

function ra_wires:fluid/source/scan

execute unless data storage ra:wires found run data modify entity @s data.status.pump_state set value "no_source"
execute unless data storage ra:wires found run return 0

function ra_wires:fluid/pump_extract with storage ra:wires found
data remove storage ra:wires found
