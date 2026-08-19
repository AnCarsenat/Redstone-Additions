# Current values on the Chunk Loader page.
#
# Records which page this is, so an action can redraw it afterwards. Per
# player, because two operators can be on different pages at once.
scoreboard players set @s ra.settings.apage 0

tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Chunk Loader",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"chunk_loader"}] run tellraw @s [{text:"  Chunk Loader: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Chunk Loader being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 3"}}]
execute if data storage ra:settings disabled[{b:"chunk_loader"}] run tellraw @s [{text:"  Chunk Loader: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Chunk Loader again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 4"}}]
tellraw @s [{text:""}]
tellraw @s [{text:"  "},{text:"[Back]",color:"gray",bold:true,hover_event:{action:"show_text",value:"Back to all server settings"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 5"}}]
