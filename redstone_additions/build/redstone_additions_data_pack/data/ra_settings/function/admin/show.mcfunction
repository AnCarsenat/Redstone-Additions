# Every operator settings page, and how to reach it.
#
# The paths are printed as clickable suggestions as well as text, because the
# point of the function tree is that it autocompletes -- this puts the first
# segment in the chat box so tab does the rest.
tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Redstone Additions — Operator Settings",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
tellraw @s [{text:"  Chunk Loader: ",color:"white"},{text:"/function ra_settings:admin/chunk_loader/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/chunk_loader/"}}]
tellraw @s [{text:"  Ender Vaults: ",color:"white"},{text:"/function ra_settings:admin/ender/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/ender/"}}]
tellraw @s [{text:"  Logic Gates: ",color:"white"},{text:"/function ra_settings:admin/gates/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/gates/"}}]
tellraw @s [{text:"  General: ",color:"white"},{text:"/function ra_settings:admin/general/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/general/"}}]
tellraw @s [{text:"  Infinite Generators: ",color:"white"},{text:"/function ra_settings:admin/infinite/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/infinite/"}}]
tellraw @s [{text:"  Interactive Blocks: ",color:"white"},{text:"/function ra_settings:admin/interactive/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/interactive/"}}]
tellraw @s [{text:"  Multiblocks: ",color:"white"},{text:"/function ra_settings:admin/multiblock/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/multiblock/"}}]
tellraw @s [{text:"  Sensors: ",color:"white"},{text:"/function ra_settings:admin/sensors/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/sensors/"}}]
tellraw @s [{text:"  Storage: ",color:"white"},{text:"/function ra_settings:admin/storage/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/storage/"}}]
tellraw @s [{text:"  Wireless Redstone: ",color:"white"},{text:"/function ra_settings:admin/wireless/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/wireless/"}}]
tellraw @s [{text:"  Power & Fluids: ",color:"white"},{text:"/function ra_settings:admin/wires/show",color:"yellow",hover_event:{action:"show_text",value:"Show current values"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/wires/"}}]
tellraw @s [{text:"Players change their own preferences with ",color:"gray"},{text:"/trigger ra.settings.open set 1",color:"yellow"},{text:" — no operator needed.",color:"gray"}]
