# /ra_wires:blocks/battery/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# The EU shown is the whole grid's, not this battery's. Charge belongs to the
# network, so two batteries on one run both read the same total — which is the
# honest answer rather than an invented split.

data modify storage ra:temp block_name set value "Battery"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/data_line {path:"available_eu",label:"Grid: ",color:"aqua",suffix:" EU",y:0.8}
function ra:tools/goggles/billboard/data_line {path:"capacity",label:"Of: ",color:"gray",suffix:" EU",y:0.6}
