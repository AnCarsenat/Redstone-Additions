# /ra:tools/wrench/filter_next
# Internal: one entry of the read-only filter.

execute unless data storage ra:wrench list[0] run return 0

data modify storage ra:wrench fq set from storage ra:wrench list[0]
function ra:tools/wrench/filter_test with storage ra:wrench fq

data remove storage ra:wrench list[0]
function ra:tools/wrench/filter_next
