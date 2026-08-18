# /ra_wires:blocks/electric_furnace/find_next
# Internal: one entry of the input scan. Context: at the block.

execute if score #ef.slot ra.wires.tmp matches 0.. run return 0
execute unless data storage ra:wires ef.scan[0] run return 0

data modify storage ra:wires ef.q.id set from storage ra:wires ef.scan[0].id
data modify storage ra:wires ef.q.slot set from storage ra:wires ef.scan[0].Slot
function ra_wires:blocks/electric_furnace/find_try with storage ra:wires ef.q

data remove storage ra:wires ef.scan[0]
function ra_wires:blocks/electric_furnace/find_next
