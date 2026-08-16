# /ra_wires:fluid/source/probe_next
# Internal: walk the source registry until one entry matches this block.

execute if data storage ra:wires found run return 0
execute unless data storage ra:wires probe.queue[0] run return 0

data modify storage ra:wires probe.cur set from storage ra:wires probe.queue[0]
data remove storage ra:wires probe.queue[0]

function ra_wires:fluid/source/probe_test with storage ra:wires probe.cur
function ra_wires:fluid/source/probe_next
