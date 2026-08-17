# /ra:tools/data_handler/props/row_bool {name,action}
# Internal: the bool row.

$tellraw @s [{text:"  $(name): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"green"},{text:"]",color:"dark_gray"},{text:" "},{text:"bool",color:"aqua"},{text:" "},{text:"[Toggle]",color:"yellow",click_event:{action:"run_command",command:"/trigger ra.dh.action set $(action)"},hover_event:{action:"show_text",value:"Flip this flag"}}]
