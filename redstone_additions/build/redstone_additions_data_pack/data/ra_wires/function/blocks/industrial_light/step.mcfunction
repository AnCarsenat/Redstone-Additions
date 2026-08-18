# /ra_wires:blocks/industrial_light/step {dx,dy,dz,mode}
# Internal: move one space along the beam. Context: at the last space handled.
#
# Recursion rather than ten written-out offsets, because each step has to be
# positioned relative to the one before it -- a macro cannot multiply its own
# argument by a counter.

scoreboard players add #il.n ra.wires.tmp 1
execute if score #il.n ra.wires.tmp matches 11.. run return 0

$execute positioned ~$(dx) ~$(dy) ~$(dz) run function ra_wires:blocks/industrial_light/space with storage ra:wires beam
