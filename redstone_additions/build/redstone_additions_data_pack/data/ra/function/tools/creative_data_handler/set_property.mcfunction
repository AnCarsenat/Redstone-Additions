# /ra:tools/creative_data_handler/set_property
# Set a property value on the target block
# Usage: First set the value, then call with property name
#   /data modify storage ra:cdh set_value set value "Hello friend !"
#   /function ra:tools/creative_data_handler/set_property {name:"message_block"}
# Macro: {name:"property_name"}
# As player.

# Copy value from storage to entity property
$data modify entity @e[tag=ra.cdh_target,limit=1] data.properties.$(name) set from storage ra:cdh set_value

$tellraw @s [{text:"[Creative Data Handler] ",color:"gold"},{text:"Set ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{nbt:"set_value",storage:"ra:cdh",color:"green"}]

# Refresh display
function ra:tools/creative_data_handler/refresh
