# /ra_sensors:blocks/tag_adder/add_tag_macro
# Macro to add tag to entities. As armor stand, at position.
# Input: $(tag) = tag to add, $(entity_selector) = entity selector string

# Anchor position to armor stand for distance calculations in selector
$execute store success score @s ra.temp at @s run execute as $(entity_selector) if entity @s[tag=!ra.custom_block] run tag @s add $(tag)

execute if score @s ra.temp matches 1.. at @s run playsound minecraft:block.note_block.bit block @a[distance=..16] ~ ~ ~ 0.5 1.5
execute if score @s ra.temp matches 0 at @s run playsound minecraft:block.note_block.bass block @a[distance=..16] ~ ~ ~ 0.5 0.5

