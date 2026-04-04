# /ra_lib:input/backend/writable_book/store_hand_item_slot
# Macro storage shape: {req:<int>,slot:<int>}

$data remove storage ra:input sessions.req_$(req).hand_item
$data modify storage ra:input sessions.req_$(req).hand_item set from entity @s Inventory[{Slot:$(slot)b}]
$data remove storage ra:input sessions.req_$(req).hand_item.Slot
