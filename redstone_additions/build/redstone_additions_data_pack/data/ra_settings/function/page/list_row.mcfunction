# /ra_settings:page/list_row {title,i}
# Internal: one page button.
#
# The trigger value is the index PLUS ONE, because /trigger cannot deliver 0 --
# a trigger score of 0 is indistinguishable from "not clicked this tick", which
# is exactly the state the dispatcher waits for. Page 0 would never open.

$tellraw @s [{text:"  "},{text:"[ $(title) ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open this page"},click_event:{action:"run_command",command:"/trigger ra.settings.open set $(i)"}}]
