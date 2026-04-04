# /ra_lib:input/backend/writable_book/tick_scan_req
# Macro storage shape: {req:<int>}

# Prevent input books from being dropped on the ground.
$kill @e[type=item,nbt={Item:{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}},distance=..8]

# Keep the request book available in hand while waiting.
$execute unless data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/give_book {req:$(req)}

$execute if data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}},"minecraft:writable_book_content":{pages:[{}]}}}] run function ra_lib:input/backend/writable_book/submit {req:$(req)}