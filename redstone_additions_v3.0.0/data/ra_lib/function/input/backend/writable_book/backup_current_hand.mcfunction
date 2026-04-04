# /ra_lib:input/backend/writable_book/backup_current_hand
# Save the player's selected hotbar slot and current hand item into this player's input session bucket.

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
execute store result storage ra:input tmp.slot int 1 run data get entity @s SelectedItemSlot
function ra_lib:input/backend/writable_book/store_hand_slot with storage ra:input tmp
execute if data entity @s SelectedItem run data modify storage ra:input tmp.item set from entity @s SelectedItem
execute unless data entity @s SelectedItem run data modify storage ra:input tmp.item set value {id:"minecraft:air",count:1}
function ra_lib:input/backend/writable_book/store_hand_item with storage ra:input tmp
data remove storage ra:input tmp
