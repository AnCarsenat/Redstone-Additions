# Mineral interval: back to the shipped default of 100.
data modify storage ra:settings global.props."mineral_generator"."cooldown" set value 100
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Mineral interval",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"mineral_generator\".\"cooldown\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
