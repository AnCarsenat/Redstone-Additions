# Current values on the Sensors page.
#
# Records which page this is, so an action can redraw it afterwards. Per
# player, because two operators can be on different pages at once.
scoreboard players set @s ra.settings.apage 9

tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Sensors",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"entity_detector"}] run tellraw @s [{text:"  Entity Detector: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Entity Detector being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 140"}}]
execute if data storage ra:settings disabled[{b:"entity_detector"}] run tellraw @s [{text:"  Entity Detector: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Entity Detector again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 141"}}]
execute unless data storage ra:settings disabled[{b:"tag_adder"}] run tellraw @s [{text:"  Tag Adder: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Tag Adder being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 142"}}]
execute if data storage ra:settings disabled[{b:"tag_adder"}] run tellraw @s [{text:"  Tag Adder: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Tag Adder again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 143"}}]
execute unless data storage ra:settings disabled[{b:"tag_remover"}] run tellraw @s [{text:"  Tag Remover: ",color:"white"},{text:"enabled ",color:"green"},{text:"[Disable]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop Tag Remover being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 144"}}]
execute if data storage ra:settings disabled[{b:"tag_remover"}] run tellraw @s [{text:"  Tag Remover: ",color:"white"},{text:"disabled ",color:"red"},{text:"[Enable]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow Tag Remover again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 145"}}]
tellraw @s [{text:""}]
tellraw @s [{text:"  "},{text:"[Back]",color:"gray",bold:true,hover_event:{action:"show_text",value:"Back to all server settings"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set 146"}}]
