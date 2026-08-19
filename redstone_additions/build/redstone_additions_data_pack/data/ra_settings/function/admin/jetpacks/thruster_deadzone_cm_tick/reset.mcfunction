# Thruster deadzone (cm/tick): back to the shipped default of 25.
data modify storage ra:settings global."jetpack_deadzone" set value 25
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Thruster deadzone (cm/tick)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"jetpack_deadzone\"",storage:"ra:settings",color:"aqua"}]
