# /ra_wireless:blocks/receiver/process
# Process Receiver logic. As armor stand, at position.
# Receives signal from emitters on the same channel (string identifier) or pulses from remote

# Get channel (default "default")
execute unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value "default"

# Store enabled state (can toggle with wrench)
execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b

# Only receive if enabled
execute unless data entity @s data.properties{enabled:1b} at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute unless data entity @s data.properties{enabled:1b} run return 0

# Check if pulsing from remote
execute if entity @s[tag=ra.pulsing] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute if entity @s[tag=ra.pulsing] run scoreboard players remove @s ra.pulse_timer 1
execute if entity @s[tag=ra.pulsing] if score @s ra.pulse_timer matches ..0 run tag @s remove ra.pulsing

# If pulsing, skip emitter check
execute if entity @s[tag=ra.pulsing] run return 1

# Check if any emitter on same channel is transmitting
# Store our channel in storage for comparison
data modify storage ra:temp receiver_channel set from entity @s data.properties.channel
tag @s add ra.checking

# Look for any transmitting emitter with matching channel
execute as @e[tag=ra.custom_block.emitter,tag=ra.transmitting] run function ra_wireless:blocks/receiver/check_emitter_channel

# Output based on receiving state
execute if entity @s[tag=ra.receiving] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute unless entity @s[tag=ra.receiving] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# Cleanup
tag @s remove ra.checking
tag @s remove ra.receiving
