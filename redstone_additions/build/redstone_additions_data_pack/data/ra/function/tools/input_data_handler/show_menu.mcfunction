# /ra:tools/input_data_handler/show_menu
# Show the input data handler menu.
# Uses /trigger buttons so non-OP players can use it.

tellraw @s ""
tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Input Data Handler",color:"aqua",bold:true},{text:" ══════",color:"dark_gray"}]
tellraw @s [{text:"Shift+RMB with this tool to reopen this menu.",color:"gray"}]
tellraw @s ""
tellraw @s [{text:"[Open Number Input]",color:"gold",click_event:{action:"run_command",command:"/trigger ra.input_handler.action set 1"},hover_event:{action:"show_text",value:"Open numeric input request (1..9)"}}]
tellraw @s [{text:"[Open Text Input]",color:"light_purple",click_event:{action:"run_command",command:"/trigger ra.input_handler.action set 2"},hover_event:{action:"show_text",value:"Give writable book and capture page 1"}}]
tellraw @s [{text:"[Poll State]",color:"yellow",click_event:{action:"run_command",command:"/trigger ra.input_handler.action set 3"},hover_event:{action:"show_text",value:"Check current input session state"}},{text:"  "},{text:"[Consume Result]",color:"green",click_event:{action:"run_command",command:"/trigger ra.input_handler.action set 4"},hover_event:{action:"show_text",value:"Consume latest ready result"}}]
tellraw @s [{text:"[Cancel Session]",color:"red",click_event:{action:"run_command",command:"/trigger ra.input_handler.action set 5"},hover_event:{action:"show_text",value:"Cancel current input session"}},{text:"  "},{text:"[Refresh]",color:"gray",click_event:{action:"run_command",command:"/trigger ra.input_handler.action set 6"},hover_event:{action:"show_text",value:"Show this menu again"}}]