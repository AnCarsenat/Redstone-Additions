# Thrust (% of speed): back to the shipped default of 80.
data modify storage ra:settings global."jetpack_thrust" set value 80
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Thrust (% of speed)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"jetpack_thrust\"",storage:"ra:settings",color:"aqua"}]
