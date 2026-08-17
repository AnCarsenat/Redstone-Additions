# /ra_lib_multiblock:generic/setup_type_run {type:"..."}
# Internal: apply ra.multiblock.{type} and mark the marker as registry-driven, so
# generic/check_structure knows it can validate it from the spec.

$execute unless data storage ra:multiblock types.$(type) run return 0

$tag @s add ra.multiblock.$(type)
data modify entity @s data.registry_driven set value 1b
