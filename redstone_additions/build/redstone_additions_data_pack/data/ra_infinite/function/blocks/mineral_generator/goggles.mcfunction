# /ra_infinite:blocks/mineral_generator/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.

data modify storage ra:temp block_name set value "Mineral Generator"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/prop_line {path:"cooldown",label:"Cooldown: ",color:"aqua",suffix:"t",y:0.75}
# enabled is a byte, so it is rendered as words rather than copied raw.
execute unless data entity @s data.properties{enabled:0b} run function ra:tools/goggles/billboard/text_line {label:"Enabled: ",value:"yes",color:"green",suffix:"",y:0.5}
execute if data entity @s data.properties{enabled:0b} run function ra:tools/goggles/billboard/text_line {label:"Enabled: ",value:"no",color:"red",suffix:"",y:0.5}
