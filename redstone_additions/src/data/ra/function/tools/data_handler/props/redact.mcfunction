# /ra:tools/data_handler/props/redact
# Build storage ra:dh display_props: everything on the block, minus the fields this
# player is not meant to see. As player.
#
# Everything is shown by default. The only thing that removes a field is being named
# in creative_only, and only for a player who is not in creative.

data modify storage ra:dh display_props set from storage ra:dh properties
execute if entity @s[gamemode=creative] run return 0

data modify storage ra:dh hide_iter set from storage ra:dh creative_only_names
function ra:tools/data_handler/props/redact_next
