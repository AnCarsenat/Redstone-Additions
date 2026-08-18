# /ra_gates:blocks/randomizer/process
# Process Randomizer logic. As armor stand, at position.
# Outputs random signal (based on chance property, default 50%) on rising edge

# Detect redstone state for this marker.
tag @s add ra.redstone.ignore_blocks
function ra_lib:redstone/detect_switch
tag @s remove ra.redstone.ignore_blocks

# Detect rising edge
execute unless entity @s[tag=ra.was_powered] if entity @s[tag=ra.powered] run function ra_gates:blocks/randomizer/trigger

# Track power state
execute if entity @s[tag=ra.powered] run tag @s add ra.was_powered
execute unless entity @s[tag=ra.powered] run tag @s remove ra.was_powered

# Turn off when power removed
execute unless entity @s[tag=ra.powered] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
