# /ra_interactive:tools/hidden_fields
# Which fields the survival Data Handler should not show. Context: as the marker.
#
# The Block Breaker and Block Placer run on a fixed one-second cooldown. It is
# machine timing, not a tuning knob: a survival player editing it to zero turns
# either block into a per-tick machine, which is exactly what the cooldown exists
# to prevent. Creative still shows everything.

execute if entity @s[tag=ra.custom_block.block_breaker] run data modify storage ra:dh hidden_names set value ["cooldown"]
execute if entity @s[tag=ra.custom_block.block_placer] run data modify storage ra:dh hidden_names set value ["cooldown"]
