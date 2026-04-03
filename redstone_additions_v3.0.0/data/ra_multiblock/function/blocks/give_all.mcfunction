# /ra_multiblock:blocks/give_all
# Give all multiblock base items

function ra_multiblock:blocks/iron_base/give
function ra_multiblock:blocks/gold_base/give
function ra_multiblock:blocks/diamond_base/give

tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given all multiblock bases!",color:"green"}]
