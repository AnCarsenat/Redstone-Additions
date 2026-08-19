# Extender duration: back to the shipped default of 20.
data modify storage ra:settings global.props."extender"."extend" set value 20
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Extender duration",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"extender\".\"extend\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
