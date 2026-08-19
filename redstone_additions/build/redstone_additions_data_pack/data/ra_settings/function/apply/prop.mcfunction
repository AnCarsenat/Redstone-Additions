# /ra_settings:apply/prop {block,prop}
# Internal: store a typed number as a block's property default.

$execute store result storage ra:settings global.props."$(block)"."$(prop)" int 1 run data get storage ra:input consume.number
$tellraw @s [{text:"[Settings] ",color:"gold"},{text:"$(block) $(prop)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"$(block)\".\"$(prop)\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
