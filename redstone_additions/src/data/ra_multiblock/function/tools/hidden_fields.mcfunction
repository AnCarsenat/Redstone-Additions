# /ra_multiblock:tools/hidden_fields
# Which fields the survival Data Handler should not show. Context: as the marker.
#
# A base's tier is what it was crafted as, and the structure check reads it.

execute if entity @s[tag=ra.custom_block.multiblock_base] run data modify storage ra:dh hidden_names set value ["tier","tier_level"]
