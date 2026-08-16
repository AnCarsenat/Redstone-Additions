# /ra_wires:fluid/source/probe {dx,dy,dz}
# Internal: test the block at the current position against the source registry.
# Stops at the first match found across the whole scan.

execute if data storage ra:wires found run return 0

data modify storage ra:wires probe.queue set from storage ra:wires source_blocks
$data modify storage ra:wires probe.dx set value $(dx)
$data modify storage ra:wires probe.dy set value $(dy)
$data modify storage ra:wires probe.dz set value $(dz)

function ra_wires:fluid/source/probe_next
data remove storage ra:wires probe
