# /ra_lib:input/backend/trigger/open_choice
# Prompt user to submit numeric input using /trigger.

tellraw @s ""
tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Numeric input requested.",color:"aqua"}]
tellraw @s [{text:"Range: ",color:"gray"},{score:{name:"@s",objective:"ra.input.min"},color:"yellow"},{text:"..",color:"dark_gray"},{score:{name:"@s",objective:"ra.input.max"},color:"yellow"}]
tellraw @s [{text:"Use ",color:"gray"},{text:"/trigger ra.input.trigger set ",color:"yellow",hover_event:{action:"show_text",value:"Enter a number in the shown range"},click_event:{action:"suggest_command",command:"/trigger ra.input.trigger set "}}]