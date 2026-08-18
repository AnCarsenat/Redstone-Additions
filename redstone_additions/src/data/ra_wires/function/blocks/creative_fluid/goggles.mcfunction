# /ra_wires:blocks/creative_fluid/goggles
# Goggles readout, and the block's display name.
# Context: as the block's marker, at the block position.

data modify storage ra:temp block_name set value "Creative Fluid Source"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/stack_reset {top:80,step:22}
function ra:tools/goggles/billboard/stacked_status_line {path:"state",label:"",color:"light_purple",suffix:""}
function ra:tools/goggles/billboard/stacked_status_line {path:"medium",label:"Medium: ",color:"aqua",suffix:""}
function ra:tools/goggles/billboard/stacked_status_line {path:"filled",label:"Filled: ",color:"yellow",suffix:" mL"}
