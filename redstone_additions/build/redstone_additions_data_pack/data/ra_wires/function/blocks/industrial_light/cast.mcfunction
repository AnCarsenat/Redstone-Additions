# /ra_wires:blocks/industrial_light/cast {mode:"on"|"off"}
# Internal: walk the beam out from this block and set or clear each space.
# Context: as the marker, at its block.
#
# ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.

data remove storage ra:wires beam
$data modify storage ra:wires beam.mode set value "$(mode)"
scoreboard players set #il.n ra.wires.tmp 0

execute if score @s ra.facing matches 0 run data modify storage ra:wires beam merge value {dx:0,dy:-1,dz:0}
execute if score @s ra.facing matches 1 run data modify storage ra:wires beam merge value {dx:0,dy:1,dz:0}
execute if score @s ra.facing matches 2 run data modify storage ra:wires beam merge value {dx:0,dy:0,dz:-1}
execute if score @s ra.facing matches 3 run data modify storage ra:wires beam merge value {dx:0,dy:0,dz:1}
execute if score @s ra.facing matches 4 run data modify storage ra:wires beam merge value {dx:-1,dy:0,dz:0}
execute if score @s ra.facing matches 5 run data modify storage ra:wires beam merge value {dx:1,dy:0,dz:0}
execute unless data storage ra:wires beam.dx run data modify storage ra:wires beam merge value {dx:0,dy:0,dz:1}

function ra_wires:blocks/industrial_light/step with storage ra:wires beam
