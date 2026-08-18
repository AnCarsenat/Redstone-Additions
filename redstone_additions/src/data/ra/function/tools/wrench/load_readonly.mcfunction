# /ra:tools/wrench/load_readonly {type}
# Internal: copy this block type's read-only set into storage ra:wrench ro.

$data modify storage ra:wrench ro set from storage ra:dh readonly."$(type)"
