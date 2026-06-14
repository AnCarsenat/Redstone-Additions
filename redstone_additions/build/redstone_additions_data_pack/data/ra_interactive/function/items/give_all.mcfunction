# /data/ra/function/items/give_all.mcfunction`
# Give all custom block items

function ra_interactive:blocks/block_breaker/give
function ra_interactive:blocks/spitter/give
function ra_interactive:blocks/block_placer/give
function ra_interactive:blocks/item_pipe/give
function ra_interactive:blocks/breeder/give
function ra_interactive:blocks/infinite_water_cauldron/give
function ra_interactive:blocks/infinite_lava_cauldron/give
function ra_interactive:blocks/infinite_snow_cauldron/give
function ra_interactive:blocks/message_block/give
function ra_interactive:blocks/item_mover/give

tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given all custom blocks!",color:"green"}]
