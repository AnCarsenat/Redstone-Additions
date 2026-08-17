# /ra_lib_multiblock:generic/check_structure
# Periodic integrity check for registry-driven multiblocks.
# Hook: #ra_lib_multiblock:check_structure
# Context: as the marker, at the base position. Sets @s ra.multiblock to 1 when
# the structure still stands.

execute unless data entity @s data{registry_driven:1b} run return 0

data modify storage ra:multiblock chk_marker.type set from entity @s data.type
data modify storage ra:multiblock chk_marker.facing set from entity @s data.facing

execute store result score @s ra.multiblock run function ra_lib_multiblock:structure/check with storage ra:multiblock chk_marker
data remove storage ra:multiblock chk_marker
