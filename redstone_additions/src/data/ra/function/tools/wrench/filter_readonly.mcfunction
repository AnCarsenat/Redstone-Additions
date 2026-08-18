# /ra:tools/wrench/filter_readonly
# Drop every read-only entry from storage ra:wrench list.
#
# Rebuilds the list rather than removing in place: deleting from a list while
# walking it shifts every index after the one removed, which is the classic way
# to skip half the entries you meant to check.

execute unless data storage ra:wrench ro run return 0

data modify storage ra:wrench keep set value []
function ra:tools/wrench/filter_next
data modify storage ra:wrench list set from storage ra:wrench keep
data remove storage ra:wrench keep
