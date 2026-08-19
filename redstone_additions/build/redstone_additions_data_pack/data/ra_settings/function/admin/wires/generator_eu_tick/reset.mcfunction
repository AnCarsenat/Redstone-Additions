# Generator EU/tick: back to the shipped default of 60.
data modify storage ra:settings global.props."electric_generator"."generation_rate" set value 60
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Generator EU/tick",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"electric_generator\".\"generation_rate\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
