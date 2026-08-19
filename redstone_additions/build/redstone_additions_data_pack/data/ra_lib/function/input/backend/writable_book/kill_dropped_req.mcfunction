# /ra_lib:input/backend/writable_book/kill_dropped_req
# Macro storage shape: {req:<int>}

$execute at @s as @e[type=item,distance=..8,nbt={Item:{id:"minecraft:writable_book"}}] if data entity @s {Item:{components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}} run kill @s
# The unqualified sweep that used to be here killed EVERY input form within eight
# blocks, not just this request's -- including one another player standing nearby
# was still writing in. Tearing down one session must not end somebody else's.
