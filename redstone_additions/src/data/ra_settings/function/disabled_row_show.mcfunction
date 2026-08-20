# /ra_settings:disabled_row_show {label,code}
# Internal: the line for one disabled block.

# The button is shown only to somebody who can press it. The dispatcher would
# refuse anyone else and the objective is not even enabled for them, so offering
# it would be offering a button whose whole behaviour is to say no.
$execute unless entity @s[tag=ra.admin] run tellraw @s [{text:"  "},{text:"$(label)",color:"red"}]
$execute if entity @s[tag=ra.admin] run tellraw @s [{text:"  "},{text:"$(label)",color:"red"},{text:"  "},{text:"[ Enable ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow $(label) to be placed again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set $(code)"}}]
