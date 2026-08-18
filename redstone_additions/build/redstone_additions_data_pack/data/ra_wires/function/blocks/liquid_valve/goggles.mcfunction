# /ra_wires:blocks/liquid_valve/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# The name is published to storage ra:temp block_name before anything is
# drawn, and the function stops there when the caller only wants the name.
# That makes this the single source of the name: ra:tools/block_name reuses
# the same dispatch to answer the Data Handlers, which used to carry two
# separate hand-written name tables that had already drifted apart.

data modify storage ra:temp block_name set value "Liquid Valve"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

# Contents belong to the network, not this node; refresh_status copies them here.

# A bridge is not a network node, so it has no medium or amount of its own -- it
# used to report the fluid-node fields and print N/A for all of them. What it can
# say is whether it is actually moving anything, and why not when it is not.
function ra:tools/goggles/billboard/data_line {path:"moved",label:"Moving: ",color:"aqua",suffix:" mL",y:0.55}
execute if data entity @s data.status{bridge_state:"transferring"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Transferring",color:"green",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"level"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Balanced",color:"aqua",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"unpowered"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"No redstone",color:"gray",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"disabled"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Disabled",color:"gray",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"needs_two_networks"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Needs a pipe on two sides",color:"red",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"same_network"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Both sides one network",color:"red",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"blocked"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Far side cannot take it",color:"yellow",suffix:"",y:0.3}
