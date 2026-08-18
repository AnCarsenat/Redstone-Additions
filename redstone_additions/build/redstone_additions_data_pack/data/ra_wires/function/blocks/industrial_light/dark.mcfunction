# /ra_wires:blocks/industrial_light/dark {why:"No EU"}
# Put the beam out and say which of the two conditions failed.
# Context: as the marker, at its block.
#
# `off` reported a bare "Dark", which is true and useless: a light that is not lit
# is either unpowered or unpaid, and those are fixed at opposite ends of the
# build. The goggles now name the one that is missing.
#
# WHY THE CLEAR IS CONDITIONAL
# This used to walk and clear the whole ten-block beam on every tick the light
# was not lit -- about sixty commands per light per tick, for ever, for lights
# that are simply switched off. A switched-off light does not need clearing
# repeatedly; it needs clearing once, on the tick it goes out.
#
# The periodic sweep is what the unconditional version was really for: a beam
# orphaned by a chunk unload has no marker left to notice the transition. Once
# every ten seconds is often enough for something that should never happen, and
# it costs a two-hundredth of what running it every tick did.

$data modify entity @s data.status.beam set value "$(why)"
data modify entity @s data.status.active set value 0b

execute if entity @s[tag=ra.wires.light_lit] run function ra_wires:blocks/industrial_light/cast {mode:0}
execute if entity @s[tag=!ra.wires.light_lit] if score #il.sweep ra.wires.tmp2 matches 0 run function ra_wires:blocks/industrial_light/cast {mode:0}
tag @s remove ra.wires.light_lit
