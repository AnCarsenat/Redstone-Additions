# /ra_wires:fluid/drain_tick
# Move fluid between the world and the network.
# Context: as a drain marker, at the drain position.
#
# The drain has two modes, cycled with the goggles tinker:
#   "drain" — take a world source into the network, same as a pump but slower
#   "place" — spend network contents putting the medium back into the world
#
# Mode "place" is what makes a fluid network worth building: it can carry lava
# from a pool to wherever you want it, or refill cauldrons on the far side of a
# base, instead of the contents only ever being a number on a billboard.

execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "drain"
execute if data entity @s data.properties{enabled:0b} run return 0

scoreboard players add @s ra.cooldown 1
execute unless score @s ra.cooldown matches 20.. run return 0
scoreboard players set @s ra.cooldown 0

execute if data entity @s data.properties{mode:"place"} run function ra_wires:fluid/drain_place
execute if data entity @s data.properties{mode:"place"} run return 0

function ra_wires:fluid/source/scan

execute unless data storage ra:wires found run data modify entity @s data.status.drain_state set value "no_source"
execute unless data storage ra:wires found run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 4
execute unless data storage ra:wires found run return 0

function ra_wires:fluid/pump_extract with storage ra:wires found
data modify entity @s data.status.drain_state set from entity @s data.status.pump_state
data remove storage ra:wires found
