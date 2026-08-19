# /ra_settings:row/bool {label,key,i,scope,obj}
# Internal: an on/off row. Global values read from storage, user values from the
# player's own score -- which is why there are two lines and not one.

$execute if data storage ra:settings cur{scope:"global"} run tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"global.$(key)",storage:"ra:settings",color:"green"},{text:"]",color:"dark_gray"},{text:"  "},{text:"[ Toggle ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Flip this for everyone"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
$execute if data storage ra:settings cur{scope:"user"} run tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{score:{name:"@s",objective:"$(obj)"},color:"green"},{text:"]",color:"dark_gray"},{text:" "},{text:"[you]",color:"aqua"},{text:"  "},{text:"[ Toggle ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Flip this for yourself only"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
