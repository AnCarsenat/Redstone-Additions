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
#
# WHY THE DRAW IS READ THROUGH util/property
# It used to be `store result … run data get entity @s data.properties.eu_use`.
# A failed read of a missing path does not error, it stores ZERO -- so a light
# whose eu_use had never been written asked the grid for nothing, got nothing
# back, and read "took 0" as "the grid cannot pay". It sat dark on a full
# battery bank and no amount of EU would ever change that, because the amount
# was never the problem. util/property keeps the default when the read fails.


execute unless function ra_lib:redstone/any run return run function ra_wires:blocks/industrial_light/dark {why:"No redstone"}

# Pay for this tick before lighting it. A grid that cannot cover the draw leaves
# the beam dark rather than running it for free.
function ra_lib:util/property {name:"eu_use",default:10,min:0}
scoreboard players operation #il.use ra.wires.tmp = #prop ra.temp

# A light configured to draw nothing is free, and must not be made to prove it
# can pay -- taking 0 always returns 0, which is the refusal value.
scoreboard players set #il.got ra.wires.tmp 1
execute if score #il.use ra.wires.tmp matches 1.. store result storage ra:wires eu.amount int 1 run scoreboard players get #il.use ra.wires.tmp
execute if score #il.use ra.wires.tmp matches 1.. store result score #il.got ra.wires.tmp run function ra_wires:electric/take_eu with storage ra:wires eu
execute if score #il.got ra.wires.tmp matches ..0 run return run function ra_wires:blocks/industrial_light/dark {why:"No EU"}

data modify entity @s data.status.active set value 1b
data modify entity @s data.status.beam set value "Lit"
tag @s add ra.wires.light_lit
function ra_wires:blocks/industrial_light/cast {mode:1}
