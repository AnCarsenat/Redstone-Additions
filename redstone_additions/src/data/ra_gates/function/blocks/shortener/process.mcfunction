# /ra_gates:blocks/shortener/process
# Process Shortener logic. As armor stand, at position.
# Shortens redstone pulse to configurable ticks

# Get pulse length from properties (default 2 ticks)
execute unless data entity @s data.properties.pulse run data modify entity @s data.properties.pulse set value 2

# Detect redstone state for this marker.
tag @s add ra.redstone.ignore_blocks
function ra_lib:redstone/detect
tag @s remove ra.redstone.ignore_blocks

# Detect rising edge (power goes from 0 to 1+)
execute unless entity @s[tag=ra.was_powered] if score @s ra.power matches 1.. run tag @s add ra.pulsing
execute unless entity @s[tag=ra.was_powered] if score @s ra.power matches 1.. run data modify entity @s data.pulse_remaining set from entity @s data.properties.pulse

# Track power state
execute if score @s ra.power matches 1.. run tag @s add ra.was_powered
execute if score @s ra.power matches 0 run tag @s remove ra.was_powered

# Output while pulsing
execute if entity @s[tag=ra.pulsing] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block

# Count down pulse
execute if entity @s[tag=ra.pulsing] store result score @s ra.temp run data get entity @s data.pulse_remaining
execute if entity @s[tag=ra.pulsing] run scoreboard players remove @s ra.temp 1
execute if entity @s[tag=ra.pulsing] store result storage ra:temp pulse_val int 1 run scoreboard players get @s ra.temp
execute if entity @s[tag=ra.pulsing] run data modify entity @s data.pulse_remaining set from storage ra:temp pulse_val

# Stop pulsing when timer runs out
execute if entity @s[tag=ra.pulsing] if score @s ra.temp matches ..0 run tag @s remove ra.pulsing
execute if entity @s[tag=ra.pulsing] if score @s ra.temp matches ..0 at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute unless entity @s[tag=ra.pulsing] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
