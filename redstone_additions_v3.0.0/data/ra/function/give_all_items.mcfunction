# /data/ra/function/give_all_items.mcfunction
# Give all custom block items and tools to the executing player

# === Tools ===
function ra:tools/wrench/give
function ra:tools/creative_data_handler/give
function ra:tools/goggles/give

# === Interactive Blocks ===
function ra_interactive:items/give_all

# === Gate Blocks ===
function ra_gates:items/give_all

# === Sensor Blocks ===
function ra_sensors:items/give_all

# === Wireless Blocks ===
function ra_wireless:items/give_all

# === Example Blocks ===
function ra_example:items/give_all

# === Chunk Loader Blocks ===
function ra_chunk_loader:items/give_all

# === Multiblock Bases ===
function ra_multiblock:blocks/give_all

tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given all custom blocks and tools!",color:"green"}]