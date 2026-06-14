# /data/ra/function/uninstall.mcfunction
# Uninstall Redstone Additions — Confirmation Prompt
# Shows a clickable confirmation dialog before performing the actual uninstall

tellraw @a [{text:"\n"},{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Are you sure you want to uninstall Redstone Additions?",color:"red",bold:true}]
tellraw @a [{text:"  "},{text:"[CONFIRM]",color:"dark_red",bold:true,click_event:{action:"run_command",command:"/function ra:uninstall/confirm"},hover_event:{action:"show_text",value:"Click to uninstall - this cannot be undone!"}},{text:"  "},{text:"[CANCEL]",color:"green",bold:true,click_event:{action:"run_command",command:"/function ra:uninstall/cancel"},hover_event:{action:"show_text",value:"Cancel uninstallation"}}]