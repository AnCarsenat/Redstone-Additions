# /ra_lib:input/backend/writable_book/restore_current_hand_req
# Macro storage shape: {req:<int>,slot:<int>,item:<item_stack>}

$execute if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:$(slot)}
$execute if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:air"}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:$(slot)}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:$(slot)b,id:"minecraft:air"}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored unless data entity @s Inventory[{Slot:$(slot)b}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:$(slot)}
$execute unless data storage ra:input tmp.restored unless data entity @s Inventory[{Slot:$(slot)b}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b
