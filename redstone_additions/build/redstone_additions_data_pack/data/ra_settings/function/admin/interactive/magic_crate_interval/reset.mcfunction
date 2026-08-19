# Magic Crate interval: back to the shipped default of 20.
data modify storage ra:settings global.props."magic_crate"."cooldown" set value 20
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Magic Crate interval",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"magic_crate\".\"cooldown\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
