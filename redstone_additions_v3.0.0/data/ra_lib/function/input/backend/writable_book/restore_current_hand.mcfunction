# /ra_lib:input/backend/writable_book/restore_current_hand
# Clear temp input books and restore the original main-hand item from buffer.

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
function ra_lib:input/backend/writable_book/restore_current_hand_prepare with storage ra:input tmp
function ra_lib:input/backend/writable_book/clear_book_req with storage ra:input tmp
data remove storage ra:input tmp
