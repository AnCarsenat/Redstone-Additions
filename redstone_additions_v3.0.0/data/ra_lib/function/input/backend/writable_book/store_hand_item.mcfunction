# /ra_lib:input/backend/writable_book/store_hand_item
# Macro storage shape: {req:<int>,item:<item_stack>}

$data modify storage ra:input sessions.req_$(req).hand_item set value $(item)
