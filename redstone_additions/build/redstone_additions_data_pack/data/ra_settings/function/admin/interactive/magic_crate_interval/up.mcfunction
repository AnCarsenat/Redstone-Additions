# Magic Crate interval: up by 5. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 20
execute if data storage ra:settings global.props."magic_crate"."cooldown" store result score #n ra.set.tmp run data get storage ra:settings global.props."magic_crate"."cooldown"
scoreboard players add #n ra.set.tmp 5
execute if score #n ra.set.tmp matches ..0 run scoreboard players set #n ra.set.tmp 1
execute if score #n ra.set.tmp matches 1201.. run scoreboard players set #n ra.set.tmp 1200
execute store result storage ra:settings global.props."magic_crate"."cooldown" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Magic Crate interval",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"magic_crate\".\"cooldown\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
