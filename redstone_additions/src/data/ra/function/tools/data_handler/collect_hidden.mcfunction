# /ra:tools/data_handler/collect_hidden
# Work out which of this block's properties are read-only.
# Fills storage ra:dh hidden, a compound of name -> 1b.
#
# One lookup keyed by the block's type, from ra:tools/readonly/init_registry.
# It used to fan out through a #ra:hidden_fields function tag into a per-module
# chain of tag tests, then walk the resulting list into a compound one name at a
# time. Storing the registry as compounds in the first place removes both halves.

data modify storage ra:dh hidden set value {}

data remove storage ra:dh ro_q
execute as @e[type=marker,tag=ra.dh_target,limit=1] run data modify storage ra:dh ro_q.type set from entity @s data.type
execute if data storage ra:dh ro_q.type run function ra:tools/data_handler/collect_hidden_at with storage ra:dh ro_q
