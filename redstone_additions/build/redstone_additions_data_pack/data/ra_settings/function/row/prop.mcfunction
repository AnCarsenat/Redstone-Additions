# /ra_settings:row/prop {label,block,prop,i,step}
# Internal: the default for a per-block property.
#
# Labelled "new" on purpose. This value is copied into a block's own properties
# when it is PLACED, so changing it does not reach back and alter machines
# already standing in the world -- see ra_settings:placement/seed. Saying so in
# the row saves an admin wondering why their existing generators did not change.

$tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"global.props.\"$(block)\".$(prop)",storage:"ra:settings",color:"aqua"},{text:"]",color:"dark_gray"},{text:" "},{text:"(new blocks)",color:"dark_gray"},{text:"  "},{text:"[ - ]",color:"red",bold:true,hover_event:{action:"show_text",value:"Down by $(step)"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}},{text:" "},{text:"[ + ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Up by $(step)"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(ip)"}}]
