# Delayer delay: back to the shipped default of 20.
data modify storage ra:settings global.props."delayer"."delay" set value 20
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Delayer delay",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"delayer\".\"delay\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
