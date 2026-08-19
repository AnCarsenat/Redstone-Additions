# Speed cap (cm/tick): up by 25.
scoreboard players set #n ra.set.tmp 350
execute if data storage ra:settings global."jetpack_speed_cap" store result score #n ra.set.tmp run data get storage ra:settings global."jetpack_speed_cap"
scoreboard players add #n ra.set.tmp 25
execute if score #n ra.set.tmp matches ..49 run scoreboard players set #n ra.set.tmp 50
execute if score #n ra.set.tmp matches 2001.. run scoreboard players set #n ra.set.tmp 2000
execute store result storage ra:settings global."jetpack_speed_cap" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Speed cap (cm/tick)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"jetpack_speed_cap\"",storage:"ra:settings",color:"aqua"}]
