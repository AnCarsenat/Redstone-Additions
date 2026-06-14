# /ra_wireless:tools/remote/check_receiver_channel
# Check if this receiver matches the remote's channel (string) and pulse it
# Context: as receiver

# Copy receiver's channel to temp for comparison
data modify storage ra:temp check_channel set from entity @s data.properties.channel

# Compare: try to set check_channel from remote_channel
# If success=0 (no modification needed = values are same), channels match
execute store success score @s ra.temp run data modify storage ra:temp check_channel set from storage ra:temp remote_channel

# If channel matches and receiver is enabled, pulse it
execute if score @s ra.temp matches 0 if data entity @s data.properties{enabled:1b} run tag @s add ra.pulsing
execute if score @s ra.temp matches 0 if data entity @s data.properties{enabled:1b} run scoreboard players set @s ra.pulse_timer 4
