# /ra:tools/wrench/filter_test {prop}
# Internal: keep this entry unless its property is read-only for this block.

$execute if data storage ra:wrench ro.$(prop) run return 0
data modify storage ra:wrench keep append from storage ra:wrench fq
