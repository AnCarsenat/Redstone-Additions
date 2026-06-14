# /ra:tools/creative_data_handler/set_type
# Change the type of a property (converts value)
# Macro: {name:"property_name",type:"str|int|bool"}
# As player.
# Note: This suggests a command since type conversion requires knowing the new value

$tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"To change type of ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" to ",color:"gray"},{text:"$(type)",color:"aqua"},{text:", use:",color:"gray"}]

$tellraw @s [{text:"/function ra:tools/creative_data_handler/set_property {name:\"$(name)\",value:<new_value>}",color:"white",click_event:{action:"suggest_command",command:"/function ra:tools/creative_data_handler/set_property {name:\"$(name)\",value:}"}}]
