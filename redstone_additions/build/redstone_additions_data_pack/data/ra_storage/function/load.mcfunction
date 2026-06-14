# /ra_storage:load
# RA Storage - register custom storage block types

function ra_storage:blocks/boxer/register_block
function ra_storage:blocks/unboxer/register_block

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Storage blocks loaded!",color:"aqua"}]