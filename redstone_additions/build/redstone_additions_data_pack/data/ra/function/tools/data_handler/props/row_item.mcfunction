# /ra:tools/data_handler/props/row_item {name,action}
# Internal: the item_name row -- a property whose value is an item id.
#
# Two buttons. [Modify] is the ordinary text form, for typing an id out or for
# setting one while empty-handed. [Set from hand] is the one people will use:
# it copies the id of whatever the player is holding, which is the whole reason
# this type exists. Filters are configured by holding up the thing you want
# filtered, not by remembering how to spell minecraft:polished_blackstone_slab.
#
# Its action is 200 + the registry index, in the same way a row's own button is
# 100 + the index -- a second action space over the same list, so no per-property
# handler is needed for it either.

$tellraw @s [{text:"  $(name): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"green"},{text:"]",color:"dark_gray"},{text:" "},{text:"item",color:"aqua"},{text:" "},{text:"[Modify]",color:"yellow",click_event:{action:"run_command",command:"/trigger ra.dh.action set $(action)"},hover_event:{action:"show_text",value:"Type an item id in the input book"}},{text:" "},{text:"[Set from hand]",color:"aqua",click_event:{action:"run_command",command:"/trigger ra.dh.action set $(hand_action)"},hover_event:{action:"show_text",value:"Copy the id of the item you are holding"}}]
