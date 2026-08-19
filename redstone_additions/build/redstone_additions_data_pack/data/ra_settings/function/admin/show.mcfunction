# The server settings index. Context: as an operator.
#
# Opening this is what grants ra.admin, so every button below can be a
# /trigger instead of a /function and stop raising a confirmation prompt on
# each click. Reaching this function at all needs permission level 2, so the
# tag cannot be obtained any other way -- see ra_settings:admin_grant.
function ra_settings:admin_grant

tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Redstone Additions — Server Settings",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
tellraw @s [{text:"  "},{text:"[Chunk Loader]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Chunk Loader"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 202"}}]
tellraw @s [{text:"  "},{text:"[Enchant Crafting]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Enchant Crafting"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 203"}}]
tellraw @s [{text:"  "},{text:"[Ender Vaults]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Ender Vaults"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 204"}}]
tellraw @s [{text:"  "},{text:"[Logic Gates]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Logic Gates"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 205"}}]
tellraw @s [{text:"  "},{text:"[General]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open General"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 206"}}]
tellraw @s [{text:"  "},{text:"[Infinite Generators]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Infinite Generators"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 207"}}]
tellraw @s [{text:"  "},{text:"[Interactive Blocks]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Interactive Blocks"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 208"}}]
tellraw @s [{text:"  "},{text:"[Jetpacks]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Jetpacks"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 209"}}]
tellraw @s [{text:"  "},{text:"[Multiblocks]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Multiblocks"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 210"}}]
tellraw @s [{text:"  "},{text:"[Sensors]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Sensors"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 211"}}]
tellraw @s [{text:"  "},{text:"[Storage]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Storage"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 212"}}]
tellraw @s [{text:"  "},{text:"[Wireless Redstone]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Wireless Redstone"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 213"}}]
tellraw @s [{text:"  "},{text:"[Power & Fluids]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open Power & Fluids"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 214"}}]
tellraw @s [{text:""}]
tellraw @s [{text:"  "},{text:"[Uninstall]",color:"dark_red",bold:true,hover_event:{action:"show_text",value:"Remove Redstone Additions from this world"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 215"}}]
tellraw @s [{text:"Players change their own preferences with ",color:"gray"},{text:"/trigger ra.settings.open",color:"yellow"},{text:" — no operator needed.",color:"gray"}]
