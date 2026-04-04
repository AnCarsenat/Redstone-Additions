# /ra:tools/data_handler/set_property
# Set a property value on the target block
# Usage: First set the value, then call with property name
#   /data modify storage ra:dh set_value set value "Hello friend !"
#   /function ra:tools/data_handler/set_property {name:"message"}
# Macro: {name:"property_name"}
# As player.

# Copy value from storage to entity property
$data modify entity @e[tag=ra.dh_target,limit=1] data.properties.$(name) set from storage ra:dh set_value

$tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Set ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{nbt:"set_value",storage:"ra:dh",color:"green"}]

# Refresh display
function ra:tools/data_handler/refresh
