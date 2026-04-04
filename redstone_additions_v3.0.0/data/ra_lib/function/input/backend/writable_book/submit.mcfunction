# /ra_lib:input/backend/writable_book/submit
# Commit text from the matching writable book into the session result.
# Macro usage: {req:<int>}

$data remove storage ra:input sessions.req_$(req).result.text

$data modify storage ra:input sessions.req_$(req).result.text set from entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}].components."minecraft:writable_book_content".pages[0].raw
$execute unless data storage ra:input sessions.req_$(req).result.text run data modify storage ra:input sessions.req_$(req).result.text set from entity @s Inventory[{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}].components."minecraft:writable_book_content".pages[0]
$execute unless data storage ra:input sessions.req_$(req).result.text run data modify storage ra:input sessions.req_$(req).result.text set value ""

$data modify storage ra:input sessions.req_$(req).state set value "ready"

function ra_lib:input/backend/writable_book/restore_current_hand

scoreboard players set @s ra.input.result 1
scoreboard players set @s ra.input.state 2
tag @s remove ra.input.active

tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Text captured.",color:"green"}]