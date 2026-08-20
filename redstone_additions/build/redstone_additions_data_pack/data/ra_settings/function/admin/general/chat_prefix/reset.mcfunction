# Chat prefix: back to the shipped default.
data modify storage ra:settings global."prefix" set value "RA"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Chat prefix",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"prefix\"",storage:"ra:settings",color:"aqua"}]
