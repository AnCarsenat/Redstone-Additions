# /ra:tools/wrench/load_entries {type}
# Internal: copy this block type's cyclable list into storage ra:wrench list.

data remove storage ra:wrench list
$data modify storage ra:wrench list set from storage ra:wrench cyclables."$(type)"
