# /ra_ender:blocks/item_vault/goggles
# Context: as the block's marker, at the block position.

data modify storage ra:temp block_name set value "Ender Item Vault"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.25}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/stack_reset {top:105,step:16}
function ra:tools/goggles/billboard/stacked_prop_line {path:"channel",label:"Channel: ",color:"light_purple",suffix:""}
function ra:tools/goggles/billboard/stacked_prop_line {path:"mode",label:"Mode: ",color:"aqua",suffix:""}
