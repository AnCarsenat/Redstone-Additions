# Thruster deadzone (cm/tick): up by 5.
scoreboard players set #n ra.set.tmp 25
execute if data storage ra:settings global."jetpack_deadzone" store result score #n ra.set.tmp run data get storage ra:settings global."jetpack_deadzone"
scoreboard players add #n ra.set.tmp 5
execute if score #n ra.set.tmp matches ..-1 run scoreboard players set #n ra.set.tmp 0
execute if score #n ra.set.tmp matches 201.. run scoreboard players set #n ra.set.tmp 200
execute store result storage ra:settings global."jetpack_deadzone" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Thruster deadzone (cm/tick)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"jetpack_deadzone\"",storage:"ra:settings",color:"aqua"}]
