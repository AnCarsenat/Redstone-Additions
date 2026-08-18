# /ra_wires:blocks/electric_furnace/find_input
# Find the first smeltable stack in the INPUT rows (slots 0-8).
# Context: at the block. Writes #ef.slot (-1 for nothing) and storage ra:wires ef.hit.
#
# Walks a copy of Items rather than the container itself, and carries the real
# slot number from each entry, because Items only holds the stacks that exist --
# the list index is not the slot index and a barrel can be non-contiguous.

scoreboard players set #ef.slot ra.wires.tmp -1
data remove storage ra:wires ef
data modify storage ra:wires ef.scan set from block ~ ~ ~ Items
function ra_wires:blocks/electric_furnace/find_next
