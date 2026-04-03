# /ra_multiblock:blocks/base_handle_placement
# MACRO FUNCTION — Handle placement for any tier multiblock base
# Called: function ... with storage ra:multiblock tier_data.{tier}
# Context: as bat, at bat position

$execute unless entity @s[tag=ra.place.multiblock_base.$(tier)] run return 0

# Place the block using library (dir_type:0 = no facing)
$function ra_lib:placement/place {block_id:"$(block_id)",block_tag:"$(block_tag)",dir_type:0}

# Initialize properties
$data modify entity @e[tag=ra.new,limit=1] data.properties.tier set value "$(tier)"
$data modify entity @e[tag=ra.new,limit=1] data.properties.tier_level set value $(tier_level)

# Add multiblock base tag for detection
tag @e[tag=ra.new,limit=1] add ra.multiblock_base

# Remove ra.new tag
$tag @e[tag=ra.custom_block.$(block_tag),tag=ra.new] remove ra.new

# Placement effects
$playsound $(place_sound) block @a[distance=..16] ~ ~ ~ $(place_volume) $(place_pitch)
particle minecraft:electric_spark ~ ~0.5 ~ 0.3 0.3 0.3 0.05 15

return 1
