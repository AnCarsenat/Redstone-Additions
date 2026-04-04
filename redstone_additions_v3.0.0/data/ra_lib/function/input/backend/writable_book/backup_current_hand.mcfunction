# /ra_lib:input/backend/writable_book/backup_current_hand
# Save the player's selected hotbar slot and current hand item into this player's input session bucket.

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
execute store result storage ra:input tmp.slot int 1 run data get entity @s SelectedItemSlot
function ra_lib:input/backend/writable_book/store_hand_slot with storage ra:input tmp
function ra_lib:input/backend/writable_book/store_hand_item_slot with storage ra:input tmp
$execute unless data storage ra:input sessions.req_$(req).hand_item if data entity @s SelectedItem run function ra_lib:input/backend/writable_book/store_hand_item_selected with storage ra:input tmp
$execute unless data storage ra:input sessions.req_$(req).hand_item run data modify storage ra:input sessions.req_$(req).hand_item set value {id:"minecraft:air",count:1}
data remove storage ra:input tmp
