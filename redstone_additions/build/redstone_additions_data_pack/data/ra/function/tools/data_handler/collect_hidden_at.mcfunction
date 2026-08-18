# /ra:tools/data_handler/collect_hidden_at {type}
# Internal: copy this block type's read-only set, if it declares one.

$data modify storage ra:dh hidden set from storage ra:dh readonly."$(type)"
