# /ra_lib:input/backend/writable_book/restore_current_hand_req
# Macro storage shape: {req:<int>,slot:<int>,item:<item_stack>}

$execute if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.$(slot) with $(item)
$execute if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:air"}] run item replace entity @s hotbar.$(slot) with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:air"}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored unless data entity @s Inventory[{Slot:$(slot)b}] run item replace entity @s hotbar.$(slot) with $(item)
$execute unless data storage ra:input tmp.restored unless data entity @s Inventory[{Slot:$(slot)b}] run data modify storage ra:input tmp.restored set value 1b
