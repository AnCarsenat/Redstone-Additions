# Thruster thrust (% of speed): up by 10.
scoreboard players set #n ra.set.tmp 80
execute if data storage ra:settings global."jetpack_thrust" store result score #n ra.set.tmp run data get storage ra:settings global."jetpack_thrust"
scoreboard players add #n ra.set.tmp 10
execute if score #n ra.set.tmp matches ..9 run scoreboard players set #n ra.set.tmp 10
execute if score #n ra.set.tmp matches 301.. run scoreboard players set #n ra.set.tmp 300
execute store result storage ra:settings global."jetpack_thrust" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Thruster thrust (% of speed)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"jetpack_thrust\"",storage:"ra:settings",color:"aqua"}]
