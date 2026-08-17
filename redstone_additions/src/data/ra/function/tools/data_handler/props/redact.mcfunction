# /ra:tools/data_handler/props/redact
# Build storage ra:dh display_props: everything on the block, minus the fields this
# player is not meant to see. As player.
#
# Everything is shown by default. The only thing that removes a field is the block
# naming it in #ra:hidden_fields, and only for a player who is not in creative.

data modify storage ra:dh display_props set from storage ra:dh properties
execute if entity @s[gamemode=creative] run return 0

data modify storage ra:dh hide_iter set from storage ra:dh hidden_names
function ra:tools/data_handler/props/redact_next
