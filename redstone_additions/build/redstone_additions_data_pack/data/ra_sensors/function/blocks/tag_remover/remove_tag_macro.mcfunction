# /ra_sensors:blocks/tag_remover/remove_tag_macro
# Macro to remove specific tag from entities. As armor stand, at position.
# Input: $(tag) = tag to remove, $(entity_selector) = entity selector string

# Anchor position to armor stand for distance calculations in selector
$execute at @s as $(entity_selector) if entity @s[tag=!ra.custom_block] run tag @s remove $(tag)

playsound minecraft:block.note_block.bit block @a[distance=..16] ~ ~ ~ 0.5 0.8
