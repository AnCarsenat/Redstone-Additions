# /ra_lib:input/backend/writable_book/store_hand_item_selected
# Compatibility helper (legacy reference)
# Macro storage shape: {req:<int>}

$data modify storage ra:input sessions.req_$(req).hand_item set from entity @s SelectedItem
