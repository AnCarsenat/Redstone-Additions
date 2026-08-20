# Clock interval: down by 2. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 20
execute if data storage ra:settings global.props."clock"."cooldown" store result score #n ra.set.tmp run data get storage ra:settings global.props."clock"."cooldown"
scoreboard players remove #n ra.set.tmp 2
execute if score #n ra.set.tmp matches ..1 run scoreboard players set #n ra.set.tmp 2
execute if score #n ra.set.tmp matches 1201.. run scoreboard players set #n ra.set.tmp 1200
execute store result storage ra:settings global.props."clock"."cooldown" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Clock interval",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"clock\".\"cooldown\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
