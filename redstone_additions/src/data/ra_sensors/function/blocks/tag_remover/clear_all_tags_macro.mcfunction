# /ra_sensors:blocks/tag_remover/clear_all_tags_macro
# Clear all tags from entities using entity_selector. As armor stand, at position.
# Input: $(entity_selector) = entity selector string

# Anchor position to armor stand for distance calculations in selector
$execute at @s as $(entity_selector) if entity @s[tag=!ra.custom_block] run data modify entity @s Tags set value []

playsound minecraft:block.note_block.bit block @a[distance=..16] ~ ~ ~ 0.5 0.5
