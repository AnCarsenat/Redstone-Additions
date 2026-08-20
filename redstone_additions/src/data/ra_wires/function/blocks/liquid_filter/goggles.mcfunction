# /ra_wires:blocks/liquid_filter/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.

data modify storage ra:temp block_name set value "Liquid Filter"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

# What it is set to let through, said in the medium's own name and colour rather
# than as the registry key -- ra_wires:media/label is what every other fluid
# block uses for this and it is why `water` reads as Water.
function ra_wires:blocks/liquid_filter/goggles_medium

function ra:tools/goggles/billboard/data_line {path:"moved",label:"Moving: ",color:"aqua",suffix:" mL",y:0.55}
execute if data entity @s data.status{bridge_state:"transferring"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Passing",color:"green",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"level"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Balanced",color:"aqua",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"unpowered"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"No redstone",color:"gray",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"needs_two_networks"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Needs a pipe on two sides",color:"red",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"same_network"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Both sides one network",color:"red",suffix:"",y:0.3}
execute if data entity @s data.status{bridge_state:"blocked"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Nothing of that medium to pass",color:"yellow",suffix:"",y:0.3}
