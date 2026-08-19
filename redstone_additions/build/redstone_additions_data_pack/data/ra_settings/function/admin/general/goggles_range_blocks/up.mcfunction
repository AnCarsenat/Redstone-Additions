# Goggles range (blocks): up by 4.
scoreboard players set #n ra.set.tmp 16
execute if data storage ra:settings global."goggles_range" store result score #n ra.set.tmp run data get storage ra:settings global."goggles_range"
scoreboard players add #n ra.set.tmp 4
execute if score #n ra.set.tmp matches ..3 run scoreboard players set #n ra.set.tmp 4
execute if score #n ra.set.tmp matches 65.. run scoreboard players set #n ra.set.tmp 64
execute store result storage ra:settings global."goggles_range" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Goggles range (blocks)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"goggles_range\"",storage:"ra:settings",color:"aqua"}]
