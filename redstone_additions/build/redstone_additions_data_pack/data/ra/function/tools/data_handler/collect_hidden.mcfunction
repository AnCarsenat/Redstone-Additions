# /ra:tools/data_handler/collect_hidden
# Ask the selected block which of its fields are not for survival players.
# As player, with a scanned target.
#
# The list belongs to the block, not to this tool. A global list of names would
# make one block's decision another's: `cooldown` is a tuning knob on a generator
# and the entire point of a Clock, and hiding the name everywhere would take the
# Clock's period away with it.
#
# Blocks contribute through #ra:hidden_fields, which runs as the target marker.
# Declaring nothing means hiding nothing.

data modify storage ra:dh hidden_names set value []
data modify storage ra:dh hidden set value {}

execute as @e[type=marker,tag=ra.dh_target,limit=1] run function #ra:hidden_fields

# The declaration is a list, because redaction has to walk it; the membership test
# in props/render wants a compound. Derive the second from the first, once per scan.
data modify storage ra:dh hide_build set from storage ra:dh hidden_names
function ra:tools/data_handler/props/hidden_next
