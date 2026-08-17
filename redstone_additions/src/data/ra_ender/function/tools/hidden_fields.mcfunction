# /ra_ender:tools/hidden_fields
# Which fields the survival Data Handler should not show. Context: as the marker.
#
# `channel` and `mode` are the whole interface and stay editable; the rate a vault
# moves things at is not something to retune by hand.

execute if entity @s[tag=ra.custom_block.ender_fluid_vault] run data modify storage ra:dh hidden_names set value ["transfer_rate"]
execute if entity @s[tag=ra.custom_block.ender_power_vault] run data modify storage ra:dh hidden_names set value ["transfer_rate"]
