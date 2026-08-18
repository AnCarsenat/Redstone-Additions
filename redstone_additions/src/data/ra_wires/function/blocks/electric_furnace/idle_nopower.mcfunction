# /ra_wires:blocks/electric_furnace/idle_nopower
# The grid could not cover one operation. Context: as the marker, at the block.
#
# The cooldown is deliberately NOT reset here, so the furnace retries next tick
# rather than waiting out a full cycle after a momentary brownout.

data modify entity @s data.status.state set value "Not enough EU"
function ra_wires:blocks/electric_furnace/idle
