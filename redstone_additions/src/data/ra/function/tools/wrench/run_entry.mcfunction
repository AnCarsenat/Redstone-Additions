# /ra:tools/wrench/run_entry {i}
# Internal: run cyclable number i. Context: as the marker, at the block.
#
# The function to call is a string in the registry, pasted into the command by
# the macro -- which is the whole reason the registry can be data rather than a
# branch per block.

$data modify storage ra:wrench pick set from storage ra:wrench list[$(i)]
execute unless data storage ra:wrench pick run return 0
function ra:tools/wrench/run_pick with storage ra:wrench pick
