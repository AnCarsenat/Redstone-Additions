# /ra_wires:blocks/electric_breaker/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# A breaker has no stored charge to report, so what matters is whether it is
# actually moving anything and, when it is not, which side is the reason.

data modify storage ra:temp block_name set value "EU Breaker"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/data_line {path:"moved",label:"Moving: ",color:"aqua",suffix:" EU",y:0.8}

execute if data entity @s data.status{bridge_state:"transferring"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Transferring",color:"green",suffix:"",y:0.55}
execute if data entity @s data.status{bridge_state:"unpowered"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"No redstone",color:"gray",suffix:"",y:0.55}
execute if data entity @s data.status{bridge_state:"disabled"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Disabled",color:"gray",suffix:"",y:0.55}
execute if data entity @s data.status{bridge_state:"needs_two_networks"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Needs a wire on two sides",color:"red",suffix:"",y:0.55}
execute if data entity @s data.status{bridge_state:"same_network"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Both sides one grid",color:"red",suffix:"",y:0.55}
execute if data entity @s data.status{bridge_state:"level"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Balanced",color:"aqua",suffix:"",y:0.55}
execute if data entity @s data.status{bridge_state:"blocked"} run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"Far side cannot take it",color:"yellow",suffix:"",y:0.55}
