# /ra_wires:fluid/drain_item_next
# Internal: walk the held-container registry until one of them empties.
# Context: as the drain marker, at the drain position.

execute if score #dr.took ra.wires.tmp matches 1 run return 0
execute unless data storage ra:wires iq.queue[0] run return 0

data modify storage ra:wires iq.cur set from storage ra:wires iq.queue[0]
data remove storage ra:wires iq.queue[0]

function ra_wires:fluid/drain_item_try with storage ra:wires iq.cur
function ra_wires:fluid/drain_item_next
