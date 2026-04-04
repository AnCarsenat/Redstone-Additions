# /ra_lib:input/backend/writable_book/restore_current_hand_drop
# Macro storage shape: {item:<item_stack>}

$execute unless data storage ra:input tmp.item{id:"minecraft:air"} run summon item ~ ~ ~ {Item:$(item),PickupDelay:0s}
