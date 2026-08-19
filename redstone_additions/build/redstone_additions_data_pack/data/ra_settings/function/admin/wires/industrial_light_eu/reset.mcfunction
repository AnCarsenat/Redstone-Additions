# Industrial Light EU: back to the shipped default of 10.
data modify storage ra:settings global.props."industrial_light"."eu_use" set value 10
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Industrial Light EU",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"industrial_light\".\"eu_use\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
