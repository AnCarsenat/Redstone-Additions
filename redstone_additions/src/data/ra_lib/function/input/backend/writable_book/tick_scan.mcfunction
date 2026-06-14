# /ra_lib:input/backend/writable_book/tick_scan
# Poll player inventory for the matching writable input book.

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
function ra_lib:input/backend/writable_book/tick_scan_req with storage ra:input tmp
data remove storage ra:input tmp