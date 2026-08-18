# /ra_wires:fluid/drain_tick
# Move fluid between the world and the network.
# Context: as a drain marker, at the drain position.
#
# The drain has three roles. Two are chosen with the goggles tinker:
#   "drain" — take a world source into the network, same as a pump but slower
#   "place" — spend network contents putting the medium back into the world
#
# The third is chosen by how you place the block: stood vertically it stops
# working on the world entirely and becomes the hand-loading point, where a
# player empties a held container into the network. See ra_wires:fluid/drain_items.
#
# Mode "place" is what makes a fluid network worth building: it can carry lava
# from a pool to wherever you want it, or refill cauldrons on the far side of a
# base, instead of the contents only ever being a number on a billboard.

execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "drain"
# Migration: this knob was briefly called `interval`. One name across the pack.
execute if data entity @s data.properties.interval unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set from entity @s data.properties.interval
data remove entity @s data.properties.interval
execute unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set value 20
execute if data entity @s data.properties{enabled:0b} run return 0

# Throughput is the cooldown between actions, cycled through three steps with the
# wrench. Compared as a score rather than baked into the selector, so the setting
# takes effect the moment it is changed.
scoreboard players add @s ra.cooldown 1
function ra_lib:util/property {name:"cooldown",default:20,min:1}
scoreboard players operation #dr.cooldown ra.wires.tmp = #prop ra.temp
execute if score @s ra.cooldown < #dr.cooldown ra.wires.tmp run return 0
scoreboard players set @s ra.cooldown 0

# ra.facing 0 is down and 1 is up: a drain on its end is the hand-loading point,
# whatever mode it is set to.
execute if score @s ra.facing matches 0..1 run return run function ra_wires:fluid/drain_items

execute if data entity @s data.properties{mode:"place"} run function ra_wires:fluid/drain_place
execute if data entity @s data.properties{mode:"place"} run return 0

function ra_wires:fluid/source/scan

execute unless data storage ra:wires found run data modify entity @s data.status.drain_state set value "no_source"
execute unless data storage ra:wires found run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 4
execute unless data storage ra:wires found run return 0

function ra_wires:fluid/pump_extract with storage ra:wires found
data modify entity @s data.status.drain_state set from entity @s data.status.pump_state
data remove storage ra:wires found
