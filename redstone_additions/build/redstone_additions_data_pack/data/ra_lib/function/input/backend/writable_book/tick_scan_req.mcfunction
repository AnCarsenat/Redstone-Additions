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

# Submit when page 1 exists AND has something on it.
#
# "Exists" alone is not enough: a book can carry an empty page, and submitting on
# that fires the instant it is handed over -- the request completes with an empty
# string before the player has typed anything, which looks exactly like it
# cancelled itself. Both empty shapes are excluded, the bare "" and the {raw:""}
# a filtering server produces.
#
# AND IT USED TO TEST pages:[{}], WHICH NEVER FIRED
# A book page is a *filterable* string. With chat filtering off -- which is to say
# on nearly every server -- the page is written as a bare string, and `{}` in an
# NBT predicate matches a COMPOUND. So the guard only recognised a page on a
# server with filtering enabled, and everywhere else the book was scanned forever
# and the text was never captured: no error, just a request that timed out.
#
# submit already reads both shapes -- pages[0].raw first, pages[0] second -- so
# only this test was wrong. Reaching the page as a PATH rather than as part of the
# item predicate matches a string and a compound alike.
$execute if data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}].components."minecraft:writable_book_content".pages[0] unless data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}},"minecraft:writable_book_content":{pages:[""]}}}] unless data entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}},"minecraft:writable_book_content":{pages:[{raw:""}]}}}] run function ra_lib:input/backend/writable_book/submit {req:$(req)}
