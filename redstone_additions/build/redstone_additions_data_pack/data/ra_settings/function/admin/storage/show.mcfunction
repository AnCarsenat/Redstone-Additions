# Current values on the Storage page.
#
# Records which page this is, so an action can redraw it afterwards. Per
# player, because two operators can be on different pages at once.
scoreboard players set @s ra.settings.apage 10

tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Storage",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"boxer"}] run tellraw @s [{text:"  Boxer: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Boxer being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 191"}}]
execute if data storage ra:settings disabled[{b:"boxer"}] run tellraw @s [{text:"  Boxer: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Boxer again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 192"}}]
execute unless data storage ra:settings disabled[{b:"unboxer"}] run tellraw @s [{text:"  Unboxer: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Unboxer being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 193"}}]
execute if data storage ra:settings disabled[{b:"unboxer"}] run tellraw @s [{text:"  Unboxer: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Unboxer again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 194"}}]
tellraw @s [{text:""}]
tellraw @s [{text:"  "},{text:"[Back]",color:"gray",bold:true,hover_event:{action:"show_text",value:"Back to all server settings"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 195"}}]
