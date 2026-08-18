# /ra_wires:tools/hidden_fields
# Which fields the survival Data Handler should not show. Context: as the marker.
#
# Throughput and tier come from the block you crafted: a copper wire moves what a
# copper wire moves. `enabled` stays editable — cutting a line is a player decision.

execute if entity @s[tag=ra.custom_block.electric_generator] run data modify storage ra:dh hidden_names set value ["generation_rate"]
execute if entity @s[tag=ra.custom_block.electric_consumer] run data modify storage ra:dh hidden_names set value ["eu_use"]
execute if entity @s[tag=ra.custom_block.solar_panel] run data modify storage ra:dh hidden_names set value ["generation_rate"]
execute if entity @s[tag=ra.custom_block.industrial_light] run data modify storage ra:dh hidden_names set value ["eu_use"]
execute if entity @s[tag=ra.custom_block.liquid_valve] run data modify storage ra:dh hidden_names set value ["rate"]
execute if entity @s[tag=ra.custom_block.gas_valve] run data modify storage ra:dh hidden_names set value ["rate"]
execute if entity @s[tag=ra.custom_block.electric_breaker] run data modify storage ra:dh hidden_names set value ["rate"]
