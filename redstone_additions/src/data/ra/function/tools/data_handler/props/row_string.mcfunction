# /ra:tools/data_handler/props/row_string {name,action}
# Internal: the string row.

$tellraw @s [{text:"  $(name): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"green"},{text:"]",color:"dark_gray"},{text:" "},{text:"string",color:"aqua"},{text:" "},{text:"[Modify]",color:"yellow",click_event:{action:"run_command",command:"/trigger ra.dh.action set $(action)"},hover_event:{action:"show_text",value:"Write new text in the input book"}}]
