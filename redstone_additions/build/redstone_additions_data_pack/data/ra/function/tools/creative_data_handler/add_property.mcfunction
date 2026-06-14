# /ra:tools/creative_data_handler/add_property
# Add a new property to the target block
# Macro: {name:"property_name",value:...,type:"str|int|bool"}
# As player.

$data modify entity @e[tag=ra.cdh_target,limit=1] data.properties.$(name) set value $(value)

$tellraw @s [{text:"[Creative Data Handler] ",color:"gold"},{text:"Added property ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{text:"$(value)",color:"green"},{text:" (",color:"gray"},{text:"$(type)",color:"aqua"},{text:")",color:"gray"}]

# Refresh display
function ra:tools/creative_data_handler/refresh
