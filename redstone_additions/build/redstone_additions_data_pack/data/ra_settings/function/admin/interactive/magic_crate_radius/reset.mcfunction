# Magic Crate radius: back to the shipped default of 8.
data modify storage ra:settings global.props."magic_crate"."radius" set value 8
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Magic Crate radius",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"magic_crate\".\"radius\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
