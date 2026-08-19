# Message Block range: back to the shipped default of 16.
data modify storage ra:settings global.props."message_block"."range" set value 16
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Message Block range",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"message_block\".\"range\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
