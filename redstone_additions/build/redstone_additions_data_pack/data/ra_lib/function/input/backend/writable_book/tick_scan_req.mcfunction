# /ra_lib:input/backend/writable_book/tick_scan_req
# Macro storage shape: {req:<int>}

# Dropping the form cancels the request. This has to be tested before anything
# else touches the item: the previous order killed the dropped book and then
# handed a fresh one straight back, which left no way out of a request short of
# waiting for the timeout.
scoreboard players set #input_dropped ra.temp 0
$execute at @s as @e[type=item,distance=..8] if data entity @s {Item:{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}} run scoreboard players set #input_dropped ra.temp 1

execute if score #input_dropped ra.temp matches 1 run function ra_lib:input/backend/writable_book/cancel_dropped
execute if score #input_dropped ra.temp matches 1 run return 0

# Keep the request book available while waiting without overwriting the current hand.
$execute unless data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}] run function ra_lib:input/backend/writable_book/give_book_safe {req:$(req)}

$execute if data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}},"minecraft:writable_book_content":{pages:[{}]}}}] run function ra_lib:input/backend/writable_book/submit {req:$(req)}
