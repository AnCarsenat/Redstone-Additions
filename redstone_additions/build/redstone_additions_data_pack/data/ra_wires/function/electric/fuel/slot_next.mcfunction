# /ra_wires:electric/fuel/slot_next
# Internal: walk the stacks in the barrel until one of them is a fuel.
# Context: as the generator marker, at its block.
#
# fq.scan is a COPY of the container's Items. The copy is consumed from the front
# while #gen.i counts the matching index in the real container, because the real
# one must not be touched until something is actually taken from it -- removing
# an entry mid-walk would shift every index after it.

execute if data entity @s data.data.burn run return 0
execute unless data storage ra:wires fq.scan[0] run return 0

data modify storage ra:wires fq.q.id set from storage ra:wires fq.scan[0].id
execute store result storage ra:wires fq.q.n int 1 run scoreboard players get #gen.i ra.wires.tmp
function ra_wires:electric/fuel/slot_try with storage ra:wires fq.q

data remove storage ra:wires fq.scan[0]
scoreboard players add #gen.i ra.wires.tmp 1
function ra_wires:electric/fuel/slot_next
