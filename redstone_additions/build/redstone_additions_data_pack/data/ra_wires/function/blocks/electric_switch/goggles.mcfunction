# /ra_wires:blocks/electric_switch/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# The name is published to storage ra:temp block_name before anything is
# drawn, and the function stops there when the caller only wants the name.
# That makes this the single source of the name: ra:tools/block_name reuses
# the same dispatch to answer the Data Handlers, which used to carry two
# separate hand-written name tables that had already drifted apart.

data modify storage ra:temp block_name set value "EU Switch"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/data_line {path:"available_eu",label:"EU: ",color:"aqua",suffix:" EU",y:0.8}
execute if data entity @s data.status{active:1b} run function ra:tools/goggles/billboard/text_line {label:"Active: ",value:"On",color:"green",suffix:"",y:0.55}
execute unless data entity @s data.status{active:1b} run function ra:tools/goggles/billboard/text_line {label:"Active: ",value:"Off",color:"gray",suffix:"",y:0.55}
