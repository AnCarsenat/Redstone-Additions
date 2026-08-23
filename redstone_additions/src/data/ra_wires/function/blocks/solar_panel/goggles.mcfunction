# /ra_wires:blocks/solar_panel/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# The name is published to storage ra:temp block_name before anything is
# drawn, and the function stops there when the caller only wants the name.
# That makes this the single source of the name: ra:tools/block_name reuses
# the same dispatch to answer the Data Handlers, which used to carry two
# separate hand-written name tables that had already drifted apart.

data modify storage ra:temp block_name set value "Solar Panel"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:0.7}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

# `fuel` says "Sunlight", "No sunlight" or "Grid full", so labelling it "Light:"
# read as nonsense the moment the grid backed up. The grid line answers that
# question properly and the sun line only ever talks about the sun.
function ra:tools/goggles/billboard/data_line {path:"available_eu",label:"EU: ",color:"aqua",suffix:" EU",y:0.7}
function ra:tools/goggles/billboard/data_line {path:"grid",label:"",color:"yellow",suffix:"",y:0.45}
function ra:tools/goggles/billboard/data_line {path:"fuel",label:"Sun: ",color:"gold",suffix:"",y:0.2}
function ra:tools/goggles/billboard/data_line {path:"output",label:"Making: ",color:"green",suffix:" EU/t",y:-0.05}
