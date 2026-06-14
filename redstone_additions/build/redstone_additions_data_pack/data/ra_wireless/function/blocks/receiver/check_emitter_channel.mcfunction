# /ra_wireless:blocks/receiver/check_emitter_channel
# Check if this emitter's channel matches the receiver's channel (stored in ra:temp receiver_channel)
# Context: as emitter

# Copy emitter's channel to temp for comparison
data modify storage ra:temp check_channel set from entity @s data.properties.channel

# Compare: try to set check_channel from receiver_channel
# If success=0 (no modification needed = values are same), channels match
execute store success score @s ra.temp run data modify storage ra:temp check_channel set from storage ra:temp receiver_channel

# If success=0 (channels match), tag receiver as receiving  
execute if score @s ra.temp matches 0 run tag @e[tag=ra.checking] add ra.receiving
