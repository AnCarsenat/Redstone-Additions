# /ra:tools/data_handler/remove_property
# Remove a property from the target block
# Macro: {name:"property_name"}
# As player.

$data remove entity @e[tag=ra.dh_target,limit=1] data.properties.$(name)

$tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Removed property ",color:"gray"},{text:"$(name)",color:"red"}]

# Refresh display
function ra:tools/data_handler/refresh
