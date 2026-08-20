# Consumer interval: back to the shipped default of 20.
data modify storage ra:settings global.props."electric_consumer"."cooldown" set value 20
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Consumer interval",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"electric_consumer\".\"cooldown\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
