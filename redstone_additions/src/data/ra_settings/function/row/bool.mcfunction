# /ra_settings:row/bool {label,key,i,scope,obj}
# Internal: an on/off row. Global values read from storage, user values from the
# player's own score -- which is why there are two lines and not one.
#
# User rows print "on"/"off" rather than the raw 1/0. A row that answers "0" to
# "Debug messages" is not obviously a setting that is switched off, and a player
# pressing Toggle and seeing 0 become 1 has to guess which of those was on.

$execute if data storage ra:settings cur{scope:"global"} run tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"global.$(key)",storage:"ra:settings",color:"green"},{text:"]",color:"dark_gray"},{text:"  "},{text:"[ Toggle ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Flip this for everyone"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
$execute if data storage ra:settings cur{scope:"user"} if score @s $(obj) matches 1.. run tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{text:"on",color:"green"},{text:"]",color:"dark_gray"},{text:" "},{text:"[you]",color:"aqua"},{text:"  "},{text:"[ Toggle ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Flip this for yourself only"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
$execute if data storage ra:settings cur{scope:"user"} unless score @s $(obj) matches 1.. run tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{text:"off",color:"red"},{text:"]",color:"dark_gray"},{text:" "},{text:"[you]",color:"aqua"},{text:"  "},{text:"[ Toggle ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Flip this for yourself only"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
