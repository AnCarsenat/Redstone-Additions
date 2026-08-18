# /ra_wires:blocks/industrial_light/off
# Put the beam out. Context: as the marker, at its block.
#
# Runs on every tick the light is not lit, not just on the tick it goes out. A
# light block left behind by a chunk unload, a broken contraption or a /kill of
# the marker's neighbour would otherwise stay lit for ever, and there is nothing
# in the world that would ever clean it up.

data modify entity @s data.status.active set value 0b
data modify entity @s data.status.beam set value "Dark"
function ra_wires:blocks/industrial_light/cast {mode:"off"}
