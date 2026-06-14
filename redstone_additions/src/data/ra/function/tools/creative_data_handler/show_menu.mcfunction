# /ra:tools/creative_data_handler/show_menu
# Show the main data handler menu. As player.

tellraw @s ""
tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Creative Data Handler",color:"light_purple",bold:true},{text:" ══════",color:"dark_gray"}]
tellraw @s [{text:"Block: ",color:"gray"},{nbt:"block_type",storage:"ra:cdh",color:"gold"}]
tellraw @s ""

# Properties header with Add and Edit All buttons
tellraw @s [{text:"─── Properties ───",color:"yellow"},{text:" "},{text:"[+Add]",color:"green",click_event:{action:"suggest_command",command:"/function ra:tools/creative_data_handler/add_property {name:\"\",value:\"\",type:\"\"}"},hover_event:{action:"show_text",value:"Add new property"}},{text:" "},{text:"[Edit All]",color:"aqua",click_event:{action:"suggest_command",command:"/data modify entity @e[tag=ra.cdh_target,limit=1] data.properties set value "},hover_event:{action:"show_text",value:"Edit all properties at once"}}]
tellraw @s [{text:"  Properties: ",color:"gray"},{nbt:"properties",storage:"ra:cdh",color:"white",click_event:{action:"suggest_command",command:"/data modify entity @e[tag=ra.cdh_target,limit=1] data.properties set value "},hover_event:{action:"show_text",value:"Click to copy and edit"}}]

# List all properties dynamically
function ra:tools/creative_data_handler/list_properties

tellraw @s ""

# Internal Data section
tellraw @s [{text:"─── Internal Data ───",color:"aqua"},{text:" "},{text:"[View]",color:"blue",click_event:{action:"run_command",command:"/function ra:tools/creative_data_handler/show_internal_data"},hover_event:{action:"show_text",value:"View raw internal data"}}]

tellraw @s ""
tellraw @s [{text:"[Refresh]",color:"gray",click_event:{action:"run_command",command:"/function ra:tools/creative_data_handler/refresh"},hover_event:{action:"show_text",value:"Refresh data view"}}]
