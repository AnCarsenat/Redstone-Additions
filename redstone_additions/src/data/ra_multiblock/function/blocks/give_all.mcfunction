# /ra_multiblock:blocks/give_all
# Give all multiblock base items

function ra_multiblock:blocks/base_give with storage ra:multiblock tier_data.copper
function ra_multiblock:blocks/base_give with storage ra:multiblock tier_data.iron
function ra_multiblock:blocks/base_give with storage ra:multiblock tier_data.gold
function ra_multiblock:blocks/base_give with storage ra:multiblock tier_data.diamond
function ra_multiblock:blocks/base_give with storage ra:multiblock tier_data.netherite

tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given all multiblock bases!",color:"green"}]
