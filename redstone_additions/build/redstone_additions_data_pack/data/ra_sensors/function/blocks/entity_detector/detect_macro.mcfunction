# /ra_sensors:blocks/entity_detector/detect_macro
# Macro function to detect entities using selector. As armor stand, at position.
# Input: $(entity_selector) = entity selector string

# Tag self to reference later, reset score
tag @s add ra.checking
scoreboard players set @s ra.temp 0

# Find matching entities, exclude custom blocks, set score on the checking armor stand
$execute at @s as $(entity_selector) unless entity @s[tag=ra.custom_block] run scoreboard players set @e[tag=ra.checking,limit=1] ra.temp 1

tag @s remove ra.checking

# Output redstone signal based on detection
fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute if score @s ra.temp matches 1 at @s run return run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
