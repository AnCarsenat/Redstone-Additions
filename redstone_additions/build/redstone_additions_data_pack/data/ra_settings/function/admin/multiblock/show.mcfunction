# Current values on the Multiblocks page.
#
# Records which page this is, so an action can redraw it afterwards. Per
# player, because two operators can be on different pages at once.
scoreboard players set @s ra.settings.apage 8

tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Multiblocks",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"multiblock_base"}] run tellraw @s [{text:"  Multiblock Base: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Multiblock Base being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 137"}}]
execute if data storage ra:settings disabled[{b:"multiblock_base"}] run tellraw @s [{text:"  Multiblock Base: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Multiblock Base again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 138"}}]
tellraw @s [{text:""}]
tellraw @s [{text:"  "},{text:"[Back]",color:"gray",bold:true,hover_event:{action:"show_text",value:"Back to all server settings"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 139"}}]
