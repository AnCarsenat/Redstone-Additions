# Show the load message: off.
data modify storage ra:settings global."welcome" set value 0b
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Show the load message",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"welcome\"",storage:"ra:settings",color:"aqua"}]
