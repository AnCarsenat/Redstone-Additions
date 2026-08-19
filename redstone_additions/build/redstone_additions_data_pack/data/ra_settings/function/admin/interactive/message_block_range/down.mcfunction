# Message Block range: down by 4. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 16
execute if data storage ra:settings global.props."message_block"."range" store result score #n ra.set.tmp run data get storage ra:settings global.props."message_block"."range"
scoreboard players remove #n ra.set.tmp 4
execute if score #n ra.set.tmp matches ..0 run scoreboard players set #n ra.set.tmp 1
execute if score #n ra.set.tmp matches 129.. run scoreboard players set #n ra.set.tmp 128
execute store result storage ra:settings global.props."message_block"."range" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Message Block range",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"message_block\".\"range\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
