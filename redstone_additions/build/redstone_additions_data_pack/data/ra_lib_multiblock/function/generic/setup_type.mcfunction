# /ra_lib_multiblock:generic/setup_type
# Tag a freshly assembled marker for a registered type.
# Hook: #ra_lib_multiblock:setup_type
# Context: as the new marker.

data modify storage ra:multiblock st.type set from entity @s data.type
function ra_lib_multiblock:generic/setup_type_run with storage ra:multiblock st
data remove storage ra:multiblock st
