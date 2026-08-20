# /ra:uninstall/warn
# Second and final warning. Still removes nothing.
#
# This one is specific. "Are you sure" twice teaches nobody anything; a list of
# what is about to be destroyed is what actually stops the wrong click.

tellraw @s [{text:"\n"},{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"LAST WARNING — this cannot be undone.",color:"dark_red",bold:true}]
tellraw @s [{text:"  Every placed machine becomes an ordinary block.",color:"gray"}]
tellraw @s [{text:"  Every network, multiblock and goggle display is destroyed.",color:"gray"}]
tellraw @s [{text:"  Every setting, including which blocks you disabled, is erased.",color:"gray"}]
tellraw @s [{text:"  There is no backup. Close this and copy your world first if you want one.",color:"yellow"}]
tellraw @s [{text:"  "},{text:"[ UNINSTALL ]",color:"dark_red",bold:true,hover_event:{action:"show_text",value:"Destroy all Redstone Additions data now"},click_event:{action:"run_command",command:"/function ra:uninstall/confirm"}},{text:"   "},{text:"[ Cancel ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Leave everything alone"},click_event:{action:"run_command",command:"/function ra:uninstall/cancel"}}]
