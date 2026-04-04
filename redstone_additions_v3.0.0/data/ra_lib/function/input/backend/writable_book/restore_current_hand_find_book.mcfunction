# /ra_lib:input/backend/writable_book/restore_current_hand_find_book
# Macro storage shape: {req:<int>,item:<item_stack>}

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:0b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:0}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:0b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:1b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:1}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:1b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:2b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:2}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:2b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:3b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:3}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:3b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:4b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:4}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:4b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:5b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:5}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:5b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:6b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:6}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:6b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:7b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:7}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:7b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:8b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_slot_from_tmp {req:$(req),slot:8}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:8b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:9b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:0}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:9b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:10b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:1}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:10b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:11b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:2}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:11b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:12b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:3}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:12b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:13b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:4}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:13b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:14b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:5}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:14b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:15b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:6}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:15b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:16b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:7}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:16b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:17b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:8}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:17b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:18b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:9}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:18b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:19b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:10}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:19b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:20b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:11}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:20b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:21b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:12}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:21b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:22b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:13}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:22b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:23b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:14}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:23b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:24b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:15}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:24b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:25b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:16}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:25b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:26b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:17}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:26b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:27b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:18}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:27b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:28b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:19}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:28b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:29b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:20}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:29b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:30b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:21}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:30b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:31b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:22}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:31b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:32b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:23}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:32b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:33b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:24}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:33b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:34b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:25}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:34b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:35b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_inventory_from_tmp {req:$(req),slot:26}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:35b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b

$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:-106b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/restore_offhand_from_tmp {req:$(req)}
$execute unless data storage ra:input tmp.restored if data entity @s Inventory[{Slot:-106b,id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] if score @s ra.temp matches 1 run data modify storage ra:input tmp.restored set value 1b
