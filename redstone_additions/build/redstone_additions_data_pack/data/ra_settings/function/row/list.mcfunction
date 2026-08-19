# /ra_settings:row/list {label,key,i,scope,obj}
# Internal: a cycle through fixed choices.
#
# The stored value is the index, so the CURRENT choice has to be looked up before
# it can be shown -- resolved into cur.show rather than pathed to directly,
# because an nbt text component cannot take a dynamic list index.

scoreboard players set #v ra.set.tmp 0
$execute if data storage ra:settings cur{scope:"global"} store result score #v ra.set.tmp run data get storage ra:settings global.$(key)
$execute if data storage ra:settings cur{scope:"user"} store result score #v ra.set.tmp run scoreboard players get @s $(obj)
execute store result storage ra:settings q.v int 1 run scoreboard players get #v ra.set.tmp
function ra_settings:row/list_show with storage ra:settings q

$tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"cur.show",storage:"ra:settings",color:"aqua"},{text:"]",color:"dark_gray"},{text:"  "},{text:"[ Cycle ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Step to the next choice"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
