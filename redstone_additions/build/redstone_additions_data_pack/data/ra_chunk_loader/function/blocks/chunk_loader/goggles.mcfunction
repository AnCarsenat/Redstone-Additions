# /ra_chunk_loader:blocks/chunk_loader/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# The name is published to storage ra:temp block_name before anything is
# drawn, and the function stops there when the caller only wants the name.
# That makes this the single source of the name: ra:tools/block_name reuses
# the same dispatch to answer the Data Handlers, which used to carry two
# separate hand-written name tables that had already drifted apart.

data modify storage ra:temp block_name set value "Chunk Loader"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

execute if entity @s[tag=ra.powered] run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"ON",color:"green",suffix:"",y:0.8}
execute unless entity @s[tag=ra.powered] run function ra:tools/goggles/billboard/text_line {label:"State: ",value:"OFF",color:"red",suffix:"",y:0.8}
