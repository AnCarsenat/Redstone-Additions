# /ra_chunk_loader:load
# RA Chunk Loader - register chunk loader custom block

function ra_chunk_loader:blocks/chunk_loader/register_block

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Chunk Loader loaded!",color:"aqua"}]
