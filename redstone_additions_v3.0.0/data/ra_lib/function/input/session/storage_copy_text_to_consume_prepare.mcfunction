# /ra_lib:input/session/storage_copy_text_to_consume_prepare
# Copy text from the player session bucket into storage ra:input consume.text.

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
function ra_lib:input/session/storage_copy_text_to_consume with storage ra:input tmp
execute unless data storage ra:input consume.text run data modify storage ra:input consume.text set value ""
data remove storage ra:input tmp