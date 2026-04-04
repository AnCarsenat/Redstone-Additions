# /ra_lib:input/backend/writable_book/open_text
# Prompt user to write text in a temporary writable book.

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
function ra_lib:input/backend/writable_book/give_book_safe with storage ra:input tmp
data remove storage ra:input tmp

tellraw @s ""
tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Text input requested.",color:"aqua"}]
tellraw @s [{text:"Write on page 1 of the Input Form book.",color:"gray"}]
tellraw @s [{text:"Close the book when done. Capture runs automatically.",color:"gray"}]