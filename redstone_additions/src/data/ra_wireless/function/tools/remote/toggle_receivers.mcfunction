# /ra_wireless:tools/remote/toggle_receivers
# Pulse all receivers on the remote's channel (string identifier)

# Get channel from held item and store in storage
data modify storage ra:temp remote_channel set from entity @s SelectedItem.components."minecraft:custom_data".ra.channel

# Pulse all receivers on this channel
execute as @e[tag=ra.custom_block.receiver] run function ra_wireless:tools/remote/check_receiver_channel

# Feedback
tellraw @s [{text:"[Remote] ",color:"red"},{text:"Pulsed receivers on channel ",color:"gray"},{nbt:"remote_channel",storage:"ra:temp",color:"yellow"}]
playsound minecraft:block.lever.click block @s ~ ~ ~ 1 1
