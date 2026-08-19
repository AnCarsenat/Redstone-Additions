# Magic Crate radius: up by 1. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 8
execute if data storage ra:settings global.props."magic_crate"."radius" store result score #n ra.set.tmp run data get storage ra:settings global.props."magic_crate"."radius"
scoreboard players add #n ra.set.tmp 1
execute if score #n ra.set.tmp matches ..4 run scoreboard players set #n ra.set.tmp 5
execute if score #n ra.set.tmp matches 33.. run scoreboard players set #n ra.set.tmp 32
execute store result storage ra:settings global.props."magic_crate"."radius" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Magic Crate radius",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"magic_crate\".\"radius\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
