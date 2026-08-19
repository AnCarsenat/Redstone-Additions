# Current values on the Wireless Redstone page.
#
# Records which page this is, so an action can redraw it afterwards. Per
# player, because two operators can be on different pages at once.
scoreboard players set @s ra.settings.apage 11

tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Wireless Redstone",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"emitter"}] run tellraw @s [{text:"  Emitter: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Emitter being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 152"}}]
execute if data storage ra:settings disabled[{b:"emitter"}] run tellraw @s [{text:"  Emitter: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Emitter again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 153"}}]
execute unless data storage ra:settings disabled[{b:"receiver"}] run tellraw @s [{text:"  Receiver: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Receiver being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 154"}}]
execute if data storage ra:settings disabled[{b:"receiver"}] run tellraw @s [{text:"  Receiver: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Receiver again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 155"}}]
tellraw @s [{text:""}]
tellraw @s [{text:"  "},{text:"[Back]",color:"gray",bold:true,hover_event:{action:"show_text",value:"Back to all server settings"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 156"}}]
