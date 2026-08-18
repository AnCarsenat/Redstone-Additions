# /ra_wires:electric/fuel/slot_try {id,n}
# Internal: is the stack at index n a fuel? Context: as the marker, at the block.
#
# One keyed lookup. An id that is not a fuel simply has no entry, and `unless
# data` on the result is the whole test -- no comparison, no search.

$data modify storage ra:wires fq.cur set from storage ra:wires fuel_map."$(id)"
execute unless data storage ra:wires fq.cur run return 0

$data modify storage ra:wires fq.cur.n set value $(n)
function ra_wires:electric/fuel/take with storage ra:wires fq.cur
