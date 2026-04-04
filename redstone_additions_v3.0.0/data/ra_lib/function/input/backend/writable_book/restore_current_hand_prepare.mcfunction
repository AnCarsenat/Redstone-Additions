# /ra_lib:input/backend/writable_book/restore_current_hand_prepare
# Macro storage shape: {req:<int>}

$data modify storage ra:input tmp.slot set from storage ra:input sessions.req_$(req).hand_slot
$data modify storage ra:input tmp.item set from storage ra:input sessions.req_$(req).hand_item

$execute unless data storage ra:input sessions.req_$(req).hand_slot run execute store result storage ra:input tmp.slot int 1 run data get entity @s SelectedItemSlot
execute unless data storage ra:input tmp.item run data modify storage ra:input tmp.item set value {id:"minecraft:air",count:1}

data modify storage ra:input tmp.restored set value 0b

function ra_lib:input/backend/writable_book/restore_current_hand_req with storage ra:input tmp
execute unless data storage ra:input tmp.restored run function ra_lib:input/backend/writable_book/restore_current_hand_find_book with storage ra:input tmp
execute unless data storage ra:input tmp.restored run function ra_lib:input/backend/writable_book/restore_current_hand_selected with storage ra:input tmp
execute unless data storage ra:input tmp.restored run function ra_lib:input/backend/writable_book/restore_current_hand_drop with storage ra:input tmp

$data remove storage ra:input sessions.req_$(req).hand_item
$data remove storage ra:input sessions.req_$(req).hand_slot
