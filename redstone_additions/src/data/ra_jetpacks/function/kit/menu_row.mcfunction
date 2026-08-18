# /ra_jetpacks:kit/menu_row {key,mute,label,toggle,drop}
# One line of the upgrade menu. Context: as the player.
#
# Three states, and only one line is ever printed: not fitted, fitted and on,
# fitted and off.

$execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{$(key):1b}}] run tellraw @s [{text:"  $(label)  ",color:"gray"},{text:"not fitted",color:"dark_gray",italic:true}]

$execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{$(key):1b}}] unless entity @s[tag=$(mute)] run tellraw @s [{text:"  $(label)  ",color:"white"},{text:"[ ON ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Switch it off without removing it"},click_event:{action:"run_command",command:"/trigger ra.jp.kits set $(toggle)"}},{text:"  ",color:"gray"},{text:"[ REMOVE ]",color:"red",bold:true,hover_event:{action:"show_text",value:"Take the kit off and get the item back"},click_event:{action:"run_command",command:"/trigger ra.jp.kits set $(drop)"}}]

$execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{$(key):1b}}] if entity @s[tag=$(mute)] run tellraw @s [{text:"  $(label)  ",color:"gray"},{text:"[ OFF ]",color:"dark_gray",bold:true,hover_event:{action:"show_text",value:"Switch it back on"},click_event:{action:"run_command",command:"/trigger ra.jp.kits set $(toggle)"}},{text:"  ",color:"gray"},{text:"[ REMOVE ]",color:"red",bold:true,hover_event:{action:"show_text",value:"Take the kit off and get the item back"},click_event:{action:"run_command",command:"/trigger ra.jp.kits set $(drop)"}}]
