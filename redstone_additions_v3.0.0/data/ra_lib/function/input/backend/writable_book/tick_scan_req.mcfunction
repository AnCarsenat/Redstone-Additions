# /ra_lib:input/backend/writable_book/tick_scan_req
# Macro storage shape: {req:<int>}

# Prevent input books from being dropped on the ground.
$function ra_lib:input/backend/writable_book/kill_dropped_req {req:$(req)}

# Keep the request book available while waiting without overwriting the current hand.
$execute unless data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/give_book_safe {req:$(req)}

$execute if data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}},"minecraft:writable_book_content":{pages:[{}]}}}] run function ra_lib:input/backend/writable_book/submit {req:$(req)}