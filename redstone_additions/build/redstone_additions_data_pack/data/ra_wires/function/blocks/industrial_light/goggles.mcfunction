# /ra_wires:blocks/industrial_light/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.

data modify storage ra:temp block_name set value "Industrial Light"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/data_line {path:"beam",label:"Beam: ",color:"yellow",suffix:"",y:0.8}
function ra:tools/goggles/billboard/data_line {path:"available_eu",label:"EU: ",color:"aqua",suffix:" EU",y:0.55}
function ra:tools/goggles/billboard/data_line {path:"grid",label:"",color:"gray",suffix:"",y:0.3}
