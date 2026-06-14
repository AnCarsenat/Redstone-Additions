# /ra_wireless:tools/remote/change_channel
# Show channel selection menu (while sneaking)

# Get current channel from held item
data modify storage ra:temp current_channel set from entity @s SelectedItem.components."minecraft:custom_data".ra.channel

# Show channel selection menu
tellraw @s ""
tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Set Channel",color:"red",bold:true},{text:" ══════",color:"dark_gray"}]
tellraw @s [{text:"Current: ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]
tellraw @s ""
tellraw @s [{text:"[Set Channel]",color:"green",click_event:{action:"suggest_command",command:"/function ra_wireless:tools/remote/set_channel {channel:\""},hover_event:{action:"show_text",value:"Click to enter channel name"}}]
tellraw @s ""
playsound minecraft:block.note_block.bell block @s ~ ~ ~ 0.5 1.2
