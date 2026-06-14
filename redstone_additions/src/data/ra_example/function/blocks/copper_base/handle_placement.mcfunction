# /ra_example:blocks/copper_base/handle_placement
# Handle example copper base placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.example.copper_base] run return 0
tag @s add ra.place.multiblock_base.copper
function ra_multiblock:blocks/base_handle_placement with storage ra:multiblock tier_data.copper
tag @s remove ra.place.multiblock_base.copper
return 1
