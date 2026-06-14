# /ra:items/bundles/give_all
# Give the full categorized bundle kit (direct namespace bundles)

function ra:items/bundles/give_tools_bundle
function ra:items/bundles/give_interactive_bundle
function ra:items/bundles/give_storage_bundle
function ra:items/bundles/give_gates_bundle
function ra:items/bundles/give_sensors_bundle
function ra:items/bundles/give_wireless_bundle
function ra:items/bundles/give_wires_bundle
function ra:items/bundles/give_chunk_loader_bundle
function ra:items/bundles/give_multiblock_bundle

tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given categorized namespace bundles!",color:"green"}]