# /ra_wires:blocks/liquid_drain/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# The name is published to storage ra:temp block_name before anything is
# drawn, and the function stops there when the caller only wants the name.
# That makes this the single source of the name: ra:tools/block_name reuses
# the same dispatch to answer the Data Handlers, which used to carry two
# separate hand-written name tables that had already drifted apart.

data modify storage ra:temp block_name set value "Liquid Drain"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

# Contents belong to the network, not this node; refresh_status copies them here.
function ra:tools/goggles/billboard/data_line {path:"medium",label:"Medium: ",color:"aqua",suffix:"",y:0.8}
function ra:tools/goggles/billboard/data_line {path:"amount",label:"Amount: ",color:"yellow",suffix:" mL",y:0.55}
function ra:tools/goggles/billboard/data_line {path:"drain_state",label:"Drain: ",color:"gray",suffix:"",y:0.3}
function ra:tools/goggles/billboard/prop_line {path:"mode",label:"Mode: ",color:"aqua",suffix:"",y:0.05}
function ra:tools/goggles/billboard/prop_line {path:"cooldown",label:"Cooldown: ",color:"gray",suffix:"t",y:-0.2}
