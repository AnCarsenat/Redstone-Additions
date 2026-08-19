# Current values on the Enchant Crafting page.
tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Enchant Crafting",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
tellraw @s [{text:"  Enchant crafting: ",color:"white"},{nbt:"global.\"enchanting\"",storage:"ra:settings",color:"aqua"},{text:"  "},{text:"[On]",color:"green",bold:true,hover_event:{action:"show_text",value:"/function ra_settings:admin/enchanting/enchant_crafting/on"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 4"}},{text:" "},{text:"[Off]",color:"red",bold:true,hover_event:{action:"show_text",value:"/function ra_settings:admin/enchanting/enchant_crafting/off"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 5"}}]
tellraw @s [{text:""}]
tellraw @s [{text:"  "},{text:"[Back]",color:"gray",bold:true,hover_event:{action:"show_text",value:"Back to all server settings"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 6"}}]
