# /ra:tools/creative_data_handler/show_internal_data
# Show the internal data of the block. As player.

tellraw @s ""
tellraw @s [{text:"═══ ",color:"dark_gray"},{text:"Internal Data",color:"aqua",bold:true},{text:" ═══",color:"dark_gray"}]
tellraw @s ""
tellraw @s [{nbt:"internal_data",storage:"ra:cdh",color:"white"}]
tellraw @s ""
tellraw @s [{text:"[Back to Menu]",color:"gray",click_event:{action:"run_command",command:"/function ra:tools/creative_data_handler/show_menu"},hover_event:{action:"show_text",value:"Return to main menu"}}]
