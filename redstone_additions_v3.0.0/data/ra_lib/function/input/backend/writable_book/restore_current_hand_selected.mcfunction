# /ra_lib:input/backend/writable_book/restore_current_hand_selected
# Macro storage shape: {req:<int>,item:<item_stack>}

$execute unless data storage ra:input tmp.restored if data entity @s SelectedItem{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}} run item replace entity @s weapon.mainhand with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s SelectedItem{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}} run data modify storage ra:input tmp.restored set value 1b
