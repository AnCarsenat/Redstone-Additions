# /ra_settings:disabled_row_show {label,code}
# Internal: the line for one disabled block.

$tellraw @s [{text:"  "},{text:"$(label)",color:"red"},{text:"  "},{text:"[ Enable ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow $(label) to be placed again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set $(code)"}}]
