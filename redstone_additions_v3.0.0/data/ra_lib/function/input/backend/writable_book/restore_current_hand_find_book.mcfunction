# /ra_lib:input/backend/writable_book/restore_current_hand_find_book
# Macro storage shape: {req:<int>,item:<item_stack>}

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:0b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.0 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:0b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:1b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.1 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:1b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:2b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.2 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:2b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:3b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.3 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:3b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:4b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.4 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:4b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:5b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.5 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:5b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:6b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.6 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:6b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:7b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.7 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:7b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:8b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run item replace entity @s hotbar.8 with $(item)
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:8b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run data modify storage ra:input tmp.restored set value 1b
