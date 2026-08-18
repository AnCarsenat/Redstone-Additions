# /ra_wires:blocks/electric_furnace/idle_empty
# Nothing smeltable in the input rows. Context: as the marker, at the block.

data modify entity @s data.status.state set value "Nothing to smelt"
function ra_wires:blocks/electric_furnace/idle
