# /ra_lib:input/backend/writable_book/store_hand_slot
# Macro storage shape: {req:<int>,slot:<int>}

$data modify storage ra:input sessions.req_$(req).hand_slot set value $(slot)
