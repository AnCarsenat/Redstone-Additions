# /ra_sensors:blocks/tag_adder/detect_macro
# Macro function to detect entities using selector. As armor stand, at position.
# Input: $(entity_selector) = entity selector string, $(range) = detection range

$execute store success score @s ra.temp run execute as $(entity_selector) if entity @s[tag=!ra.custom_block]

# Output redstone signal based on detection
fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute if score @s ra.temp matches 1 at @s run return run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
