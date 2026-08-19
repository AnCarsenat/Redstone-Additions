# Poppy interval: up by 10. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 100
execute if data storage ra:settings global.props."poppy_generator"."cooldown" store result score #n ra.set.tmp run data get storage ra:settings global.props."poppy_generator"."cooldown"
scoreboard players add #n ra.set.tmp 10
execute if score #n ra.set.tmp matches ..0 run scoreboard players set #n ra.set.tmp 1
execute if score #n ra.set.tmp matches 1201.. run scoreboard players set #n ra.set.tmp 1200
execute store result storage ra:settings global.props."poppy_generator"."cooldown" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Poppy interval",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"poppy_generator\".\"cooldown\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
