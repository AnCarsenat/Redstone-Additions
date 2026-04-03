# /ra_example:blocks/netherite_base/handle_placement
# Handle example netherite base placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.example.netherite_base] run return 0
tag @s add ra.place.multiblock_base.netherite
function ra_multiblock:blocks/base_handle_placement with storage ra:multiblock tier_data.netherite
tag @s remove ra.place.multiblock_base.netherite
return 1
