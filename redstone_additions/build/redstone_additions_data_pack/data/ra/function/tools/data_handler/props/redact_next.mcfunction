# /ra:tools/data_handler/props/redact_next
# Internal: drop one hidden field from the display copy, then the rest.

execute unless data storage ra:dh hide_iter[0] run return 0

data modify storage ra:dh hide_q set value {}
data modify storage ra:dh hide_q.name set from storage ra:dh hide_iter[0]
function ra:tools/data_handler/props/redact_one with storage ra:dh hide_q

data remove storage ra:dh hide_iter[0]
function ra:tools/data_handler/props/redact_next
