# /ra_gates:blocks/randomizer/process
# Process Randomizer logic. As armor stand, at position.
# Outputs random signal (based on chance property, default 50%) on rising edge

# Detect redstone state for this marker.
tag @s add ra.redstone.ignore_blocks
function ra_lib:redstone/detect
tag @s remove ra.redstone.ignore_blocks

# Detect rising edge
execute unless entity @s[tag=ra.was_powered] if score @s ra.power matches 1.. run function ra_gates:blocks/randomizer/trigger

# Track power state
execute if score @s ra.power matches 1.. run tag @s add ra.was_powered
execute if score @s ra.power matches 0 run tag @s remove ra.was_powered

# Turn off when power removed
execute if score @s ra.power matches 0 at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
