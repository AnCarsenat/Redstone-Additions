# /ra_ender:blocks/fluid_vault/goggles
# Context: as the block's marker, at the block position.

data modify storage ra:temp block_name set value "Ender Fluid Vault"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.4}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/stack_reset {top:120,step:16}
function ra:tools/goggles/billboard/stacked_prop_line {path:"channel",label:"Channel: ",color:"light_purple",suffix:""}
function ra:tools/goggles/billboard/stacked_prop_line {path:"mode",label:"Mode: ",color:"aqua",suffix:""}
function ra:tools/goggles/billboard/stacked_prop_line {path:"transfer_rate",label:"Rate: ",color:"yellow",suffix:"/s"}

# What the local network holds, read live rather than stored on the marker.
function ra_lib:transport/net/read
execute store result score #ender.amount ra.temp run scoreboard players get #net_amount ra.tr.tmp
data modify storage ra:temp ender_line set value {label:"Holding: ",value:"empty",color:"gray",suffix:""}
execute if data storage ra:transport cur.medium run data modify storage ra:temp ender_line.value set from storage ra:transport cur.medium
execute if data storage ra:transport cur.medium run data modify storage ra:temp ender_line.color set value "aqua"
function ra:tools/goggles/billboard/stacked_text_line with storage ra:temp ender_line
