# Goggles range (blocks): back to the shipped default of 16.
data modify storage ra:settings global."goggles_range" set value 16
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Goggles range (blocks)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"goggles_range\"",storage:"ra:settings",color:"aqua"}]
