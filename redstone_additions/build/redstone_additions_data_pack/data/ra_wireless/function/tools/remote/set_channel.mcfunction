# /ra_wireless:tools/remote/set_channel
# Set remote channel from menu click
# Called with macro $(channel)

# Check if player is holding the remote
execute unless data entity @s SelectedItem.components."minecraft:custom_data".ra.remote run return fail

# Update the item with the new channel
$function ra_wireless:tools/remote/update_item {channel:$(channel)}

# Feedback
$tellraw @s [{text:"[Remote] ",color:"red"},{text:"Channel set to ",color:"gray"},{text:"$(channel)",color:"yellow"}]
playsound minecraft:block.note_block.pling block @s ~ ~ ~ 0.5 1.5
