# /ra_lib:input/backend/writable_book/give_book
# Macro storage shape: {req:<int>}

$item replace entity @s weapon.mainhand with minecraft:writable_book[item_name="Input Form",lore=[{text:"Write text on page 1",italic:false,color:"gray"},{text:"Close the book to submit",italic:false,color:"gray"}],custom_data={ra:{input_book:1b,input_req:$(req)}}]