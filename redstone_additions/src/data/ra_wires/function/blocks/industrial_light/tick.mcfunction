# /ra_wires:blocks/industrial_light/tick
# Project a beam of light while powered and paid for.
# Context: as the light's marker, at its block.
#
# Two conditions, both required: a redstone signal to say you want it on, and EU
# on the grid to pay for it. Losing either puts the beam out, which is the point
# of it being an electric block rather than a lamp.
#
# The beam is minecraft:light, the invisible block the game uses for its own
# lighting fixes. It is not a light source you can craft, so anything found in
# the beam's path is either ours or something a player placed deliberately with a
# command -- and either way the only block this will ever remove is a light.

execute if data entity @s data.properties{enabled:0b} run return run function ra_wires:blocks/industrial_light/off

execute unless function ra_lib:redstone/any run return run function ra_wires:blocks/industrial_light/off

# Pay for this tick before lighting it. A grid that cannot cover the draw leaves
# the beam dark rather than running it for free.
execute store result storage ra:wires eu.amount int 1 run data get entity @s data.properties.eu_use 1
execute store result score #il.got ra.wires.tmp run function ra_wires:electric/take_eu with storage ra:wires eu
execute if score #il.got ra.wires.tmp matches ..0 run return run function ra_wires:blocks/industrial_light/off

data modify entity @s data.status.active set value 1b
data modify entity @s data.status.beam set value "Lit"
function ra_wires:blocks/industrial_light/cast {mode:"on"}
