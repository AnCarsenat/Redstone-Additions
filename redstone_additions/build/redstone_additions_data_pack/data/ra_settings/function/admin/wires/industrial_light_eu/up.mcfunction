# Industrial Light EU: up by 5. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 10
execute if data storage ra:settings global.props."industrial_light"."eu_use" store result score #n ra.set.tmp run data get storage ra:settings global.props."industrial_light"."eu_use"
scoreboard players add #n ra.set.tmp 5
execute if score #n ra.set.tmp matches ..-1 run scoreboard players set #n ra.set.tmp 0
execute if score #n ra.set.tmp matches 1001.. run scoreboard players set #n ra.set.tmp 1000
execute store result storage ra:settings global.props."industrial_light"."eu_use" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Industrial Light EU",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"industrial_light\".\"eu_use\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
