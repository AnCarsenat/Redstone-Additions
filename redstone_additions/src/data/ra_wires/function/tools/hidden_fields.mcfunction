# /ra_wires:tools/hidden_fields
# Which fields the survival Data Handler should not show. Context: as the marker.
#
# Throughput and tier come from the block you crafted: a copper wire moves what a
# copper wire moves. `enabled` stays editable — cutting a line is a player decision.

execute if entity @s[tag=ra.custom_block.electric_wire] run data modify storage ra:dh hidden_names set value ["transfer_rate","tier"]
execute if entity @s[tag=ra.custom_block.electric_generator] run data modify storage ra:dh hidden_names set value ["transfer_rate","generation_rate"]
execute if entity @s[tag=ra.custom_block.electric_consumer] run data modify storage ra:dh hidden_names set value ["transfer_rate","eu_use"]
execute if entity @s[tag=ra.custom_block.electric_switch] run data modify storage ra:dh hidden_names set value ["transfer_rate"]
execute if entity @s[tag=ra.custom_block.solar_panel] run data modify storage ra:dh hidden_names set value ["transfer_rate","generation_rate"]
execute if entity @s[tag=ra.custom_block.liquid_pipe] run data modify storage ra:dh hidden_names set value ["tier"]
