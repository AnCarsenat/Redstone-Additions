# Goggles redraw (ticks): up by 5.
scoreboard players set #n ra.set.tmp 20
execute if data storage ra:settings global."goggles_redraw" store result score #n ra.set.tmp run data get storage ra:settings global."goggles_redraw"
scoreboard players add #n ra.set.tmp 5
execute if score #n ra.set.tmp matches ..4 run scoreboard players set #n ra.set.tmp 5
execute if score #n ra.set.tmp matches 101.. run scoreboard players set #n ra.set.tmp 100
execute store result storage ra:settings global."goggles_redraw" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Goggles redraw (ticks)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"goggles_redraw\"",storage:"ra:settings",color:"aqua"}]
