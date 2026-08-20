# Thruster speed cap (cm/tick): back to the shipped default of 350.
data modify storage ra:settings global."jetpack_speed_cap" set value 350
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Thruster speed cap (cm/tick)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"jetpack_speed_cap\"",storage:"ra:settings",color:"aqua"}]
