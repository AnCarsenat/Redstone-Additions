# /ra:tools/data_handler/props/row_list {name,action}
# Internal: the list row.

$tellraw @s [{text:"  $(name): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"green"},{text:"]",color:"dark_gray"},{text:" "},{text:"list",color:"aqua"},{text:" "},{text:"[Edit list]",color:"yellow",click_event:{action:"run_command",command:"/trigger ra.dh.action set $(action)"},hover_event:{action:"show_text",value:"Write the whole list, e.g. ["A","B"]"}}]
