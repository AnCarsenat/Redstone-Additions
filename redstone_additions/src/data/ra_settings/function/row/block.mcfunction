# /ra_settings:row/block {label,block,i}
# Internal: whether a block type may be placed at all.
#
# Shown as ON/OFF text rather than the raw list membership, because the stored
# form is "which blocks are disabled" and printing that reads backwards.

scoreboard players set #on ra.set.tmp 1
$execute if data storage ra:settings disabled[{b:"$(block)"}] run scoreboard players set #on ra.set.tmp 0

$execute if score #on ra.set.tmp matches 1 run tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{text:"enabled",color:"green"},{text:"]",color:"dark_gray"},{text:"  "},{text:"[ Disable ]",color:"red",bold:true,hover_event:{action:"show_text",value:"Stop this block being placed"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
$execute if score #on ra.set.tmp matches 0 run tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{text:"disabled",color:"red"},{text:"]",color:"dark_gray"},{text:"  "},{text:"[ Enable ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow this block again"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(i)"}}]
