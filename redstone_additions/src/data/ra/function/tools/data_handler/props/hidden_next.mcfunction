# /ra:tools/data_handler/props/hidden_next
# Internal: turn the declared list of hidden names into a compound to test against.

execute unless data storage ra:dh hide_build[0] run return 0

data modify storage ra:dh hide_q set value {}
data modify storage ra:dh hide_q.name set from storage ra:dh hide_build[0]
function ra:tools/data_handler/props/hidden_mark with storage ra:dh hide_q

data remove storage ra:dh hide_build[0]
function ra:tools/data_handler/props/hidden_next
