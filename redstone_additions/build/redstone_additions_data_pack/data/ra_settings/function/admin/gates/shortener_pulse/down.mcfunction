# Shortener pulse: down by 1. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 2
execute if data storage ra:settings global.props."shortener"."pulse" store result score #n ra.set.tmp run data get storage ra:settings global.props."shortener"."pulse"
scoreboard players remove #n ra.set.tmp 1
execute if score #n ra.set.tmp matches ..0 run scoreboard players set #n ra.set.tmp 1
execute if score #n ra.set.tmp matches 201.. run scoreboard players set #n ra.set.tmp 200
execute store result storage ra:settings global.props."shortener"."pulse" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Shortener pulse",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"shortener\".\"pulse\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
