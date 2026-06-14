# /ra_sensors:items/give_all
# Give all sensor block items

function ra_sensors:blocks/entity_detector/give
function ra_sensors:blocks/tag_adder/give
function ra_sensors:blocks/tag_remover/give

tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given all sensor blocks!",color:"light_purple"}]
