# /data/ra_sensors/function/load.mcfunction
# RA Sensors — Load and register entity detection and tag manipulation blocks

# Register sensor blocks
function ra_sensors:blocks/entity_detector/register_block
function ra_sensors:blocks/tag_adder/register_block
function ra_sensors:blocks/tag_remover/register_block

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Sensors loaded!",color:"aqua"}]
