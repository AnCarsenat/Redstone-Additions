# /data/ra_interactive/function/load.mcfunction
# RA Interactive — Load and register all interactive custom blocks

# Register custom blocks
function ra_interactive:blocks/block_breaker/register_block
function ra_interactive:blocks/breeder/register_block
function ra_interactive:blocks/block_placer/register_block
function ra_interactive:blocks/spitter/register_block
function ra_interactive:blocks/item_pipe/register_block
function ra_interactive:blocks/infinite_water_cauldron/register_block
function ra_interactive:blocks/infinite_lava_cauldron/register_block
function ra_interactive:blocks/infinite_snow_cauldron/register_block
function ra_interactive:blocks/message/register_block
function ra_interactive:blocks/item_mover/register_block

# Tellraw to all players to indicate that interactive blocks have been loaded
tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Interactive blocks loaded!",color:"aqua"}]
