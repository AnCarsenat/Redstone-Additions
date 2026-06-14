# /ra:tools/data_handler/show_menu
# Show the main data handler menu. As player.

tellraw @s ""
tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Data Handler",color:"aqua",bold:true},{text:" ══════",color:"dark_gray"}]
tellraw @s [{text:"Block: ",color:"gray"},{nbt:"block_type",storage:"ra:dh",color:"gold"}]
tellraw @s [{text:"Shift+RMB with this tool to rescan a block.",color:"gray"}]
tellraw @s ""

# Properties section
tellraw @s [{text:"─── Properties ───",color:"yellow"}]
tellraw @s [{text:"  Properties: ",color:"gray"},{nbt:"properties",storage:"ra:dh",color:"white"}]

# List all properties dynamically
function ra:tools/data_handler/list_properties

tellraw @s ""

# Internal Data section
tellraw @s [{text:"─── Internal Data ───",color:"aqua"},{text:" "},{text:"[View]",color:"blue",click_event:{action:"run_command",command:"/trigger ra.dh.action set 90"},hover_event:{action:"show_text",value:"View raw internal data"}}]

tellraw @s ""
tellraw @s [{text:"[Refresh]",color:"gray",click_event:{action:"run_command",command:"/trigger ra.dh.action set 91"},hover_event:{action:"show_text",value:"Refresh data view"}},{text:"  "},{text:"[Cancel Input]",color:"red",click_event:{action:"run_command",command:"/trigger ra.dh.action set 93"},hover_event:{action:"show_text",value:"Cancel current property edit"}}]
