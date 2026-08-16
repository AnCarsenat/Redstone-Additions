# /ra_lib:inventory/move_slot {src:"~ ~ ~",src_slot:N,dst_slot:M}
# Move one whole slot from a source container into the container at the current
# position. The destination slot must already be empty.
#
# This is the primitive the rest of the library should be built on. `/item
# replace ... from block ...` copies a stack across verbatim — every component,
# the exact count — with no loot table to parse and no NBT arithmetic. The older
# path went through `loot insert` with a hand-built loot table that reconstructed
# the item from an id, a count and a components blob, one item at a time.

$item replace block ~ ~ ~ container.$(dst_slot) from block $(src) container.$(src_slot)
$item replace block $(src) container.$(src_slot) with air
