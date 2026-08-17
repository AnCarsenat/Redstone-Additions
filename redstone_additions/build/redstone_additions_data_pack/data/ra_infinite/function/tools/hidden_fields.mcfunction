# /ra_infinite:tools/hidden_fields
# Which fields the survival Data Handler should not show. Context: as the marker.
#
# A generator's `cooldown` is its period, set by the recipe designer rather than by
# whoever is holding it. The Goggles still report it — this is about editing.

execute if entity @s[tag=ra.custom_block.mineral_generator] run data modify storage ra:dh hidden_names set value ["cooldown"]
execute if entity @s[tag=ra.custom_block.nether_generator] run data modify storage ra:dh hidden_names set value ["cooldown"]
execute if entity @s[tag=ra.custom_block.poppy_generator] run data modify storage ra:dh hidden_names set value ["cooldown"]
