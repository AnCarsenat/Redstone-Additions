# /ra_lib_multiblock:build/next
# Internal: pop one type id off the build queue and expand it.
#
# The queue is a sibling of `build` rather than a field inside it: `build` is
# handed to macro functions as their argument compound, and every field in such
# a compound is stringified on each call.

execute unless data storage ra:multiblock build_q[0] run return 0

data modify storage ra:multiblock build.type set from storage ra:multiblock build_q[0]
data remove storage ra:multiblock build_q[0]

function ra_lib_multiblock:build/type with storage ra:multiblock build
function ra_lib_multiblock:build/next
