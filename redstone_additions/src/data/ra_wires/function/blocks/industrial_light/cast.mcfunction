# /ra_wires:blocks/industrial_light/cast {mode:1}  -- 1 lights, 0 clears
# Internal: walk the beam out from this block and set or clear each space.
# Context: as the marker, at its block.
#
# ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. The marker is given
# one at placement (see ra_lib:placement/place), so this never has to ask a
# player where they were looking.
#
# The mode travels as a SCORE and arrives as a NUMBER, not as the word "on". It
# used to be `beam.mode`, tested in `space` with
# `if data storage ra:wires beam{mode:"on"}`, on lines that began with `$` while
# containing no macro placeholder at all -- two constructs whose behaviour I
# could not confirm from outside the game, sitting in the exact path that was
# doing nothing. A number substituted into a scoreboard set, compared with
# `matches`, has no such doubt attached to it.

data remove storage ra:wires beam
scoreboard players set #il.n ra.wires.tmp 0

$scoreboard players set #il.mode ra.wires.tmp2 $(mode)

execute if score @s ra.facing matches 0 run data modify storage ra:wires beam set value {dx:0,dy:-1,dz:0}
execute if score @s ra.facing matches 1 run data modify storage ra:wires beam set value {dx:0,dy:1,dz:0}
execute if score @s ra.facing matches 2 run data modify storage ra:wires beam set value {dx:0,dy:0,dz:-1}
execute if score @s ra.facing matches 3 run data modify storage ra:wires beam set value {dx:0,dy:0,dz:1}
execute if score @s ra.facing matches 4 run data modify storage ra:wires beam set value {dx:-1,dy:0,dz:0}
execute if score @s ra.facing matches 5 run data modify storage ra:wires beam set value {dx:1,dy:0,dz:0}

# A marker with no facing score at all would otherwise leave dx/dy/dz undefined
# and the macro in `step` would fail on a missing variable.
execute unless data storage ra:wires beam.dx run data modify storage ra:wires beam set value {dx:0,dy:0,dz:1}

function ra_wires:blocks/industrial_light/step with storage ra:wires beam
