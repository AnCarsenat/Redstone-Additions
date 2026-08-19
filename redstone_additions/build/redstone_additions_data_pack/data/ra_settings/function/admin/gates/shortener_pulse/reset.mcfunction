# Shortener pulse: back to the shipped default of 2.
data modify storage ra:settings global.props."shortener"."pulse" set value 2
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Shortener pulse",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"shortener\".\"pulse\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
