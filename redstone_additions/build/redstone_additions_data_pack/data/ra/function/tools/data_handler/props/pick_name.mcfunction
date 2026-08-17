# /ra:tools/data_handler/props/pick_name {i}
# Internal: resolve a row index to the property name it stands for.

data remove storage ra:dh pending_name
$data modify storage ra:dh pending_name set from storage ra:dh registry[$(i)]
