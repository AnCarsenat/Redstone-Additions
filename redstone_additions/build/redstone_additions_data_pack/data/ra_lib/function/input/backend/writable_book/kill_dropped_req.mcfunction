# /ra_lib:input/backend/writable_book/kill_dropped_req
# Macro storage shape: {req:<int>}

$execute at @s as @e[type=item,distance=..8,nbt={Item:{id:"minecraft:writable_book"}}] if data entity @s {Item:{components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}} run kill @s
execute at @s as @e[type=item,distance=..8,nbt={Item:{id:"minecraft:writable_book"}}] if data entity @s {Item:{components:{"minecraft:custom_data":{ra:{input_book:1b}}}}} run kill @s
