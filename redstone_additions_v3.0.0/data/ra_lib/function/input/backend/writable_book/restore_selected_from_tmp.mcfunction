# /ra_lib:input/backend/writable_book/restore_selected_from_tmp
# Macro storage shape: {req:<int>}

summon minecraft:armor_stand ~ ~ ~ {Marker:1b,Invisible:1b,NoGravity:1b,Invulnerable:1b,Silent:1b,Tags:["ra.input.restore_tmp","ra.input.restore_tmp_new"]}
data modify entity @e[type=minecraft:armor_stand,tag=ra.input.restore_tmp_new,limit=1,sort=nearest] HandItems[0] set from storage ra:input tmp.item
scoreboard players set @s ra.temp 0

$execute unless data storage ra:input tmp.item{id:"minecraft:air"} store success score @s ra.temp run item replace entity @s weapon.mainhand from entity @e[type=minecraft:armor_stand,tag=ra.input.restore_tmp_new,limit=1,sort=nearest] weapon.mainhand

kill @e[type=minecraft:armor_stand,tag=ra.input.restore_tmp_new,limit=1,sort=nearest]
