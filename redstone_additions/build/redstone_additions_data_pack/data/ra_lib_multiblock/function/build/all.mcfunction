# /ra_lib_multiblock:build/all
# Derive the rotated facing variants of every registered structure.
# Runs once per load, straight after #ra_lib_multiblock:load.

data modify storage ra:multiblock build_q set from storage ra:multiblock registry
function ra_lib_multiblock:build/next

data remove storage ra:multiblock build_q
data remove storage ra:multiblock build
data remove storage ra:multiblock work_q
data remove storage ra:multiblock work
data remove storage ra:multiblock work_cur
