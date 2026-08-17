# /ra_infinite:load
# RA Infinite — generators that grow their own material, forever

function ra_infinite:blocks/mineral_generator/register_block
function ra_infinite:blocks/nether_generator/register_block
function ra_infinite:blocks/poppy_generator/register_block

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Infinite generators loaded!",color:"aqua"}]
