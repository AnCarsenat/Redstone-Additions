# /ra:uninstall
# First of two warnings. Nothing is removed by this function.
#
# Two prompts rather than one because this is unrecoverable and reachable from a
# button on the settings index -- one misclick should not be able to end a world's
# worth of machines. The first asks; the second says exactly what goes.

tellraw @s [{text:"\n"},{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Uninstall Redstone Additions?",color:"red",bold:true}]
tellraw @s [{text:"  This removes every custom block, marker and display in the world.",color:"gray"}]
tellraw @s [{text:"  "},{text:"[ Continue ]",color:"red",bold:true,hover_event:{action:"show_text",value:"You will be asked once more"},click_event:{action:"run_command",command:"/function ra:uninstall/warn"}},{text:"   "},{text:"[ Cancel ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Leave everything alone"},click_event:{action:"run_command",command:"/function ra:uninstall/cancel"}}]
