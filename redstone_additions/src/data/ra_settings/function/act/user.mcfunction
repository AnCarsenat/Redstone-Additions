# /ra_settings:act/user {obj,type,d,default,min,max,step,values}
# Internal: change this player's own value.

scoreboard players set #n ra.set.tmp 0
$execute if score @s $(obj) matches -2147483648.. store result score #n ra.set.tmp run scoreboard players get @s $(obj)

# bool: flip it.
$execute if data storage ra:settings cur{type:"bool"} if score #n ra.set.tmp matches 1.. run scoreboard players set @s $(obj) 0
$execute if data storage ra:settings cur{type:"bool"} if score #n ra.set.tmp matches ..0 run scoreboard players set @s $(obj) 1

# int: step, then clamp. Clamping here rather than refusing keeps the button
# always doing something visible at the ends of the range.
$execute if data storage ra:settings cur{type:"int"} if score #dir ra.set.tmp matches 1 run scoreboard players add #n ra.set.tmp $(step)
$execute if data storage ra:settings cur{type:"int"} if score #dir ra.set.tmp matches ..-1 run scoreboard players remove #n ra.set.tmp $(step)
$execute if data storage ra:settings cur{type:"int"} run scoreboard players set #lo ra.set.tmp $(min)
$execute if data storage ra:settings cur{type:"int"} run scoreboard players set #hi ra.set.tmp $(max)
execute if data storage ra:settings cur{type:"int"} if score #n ra.set.tmp < #lo ra.set.tmp run scoreboard players operation #n ra.set.tmp = #lo ra.set.tmp
execute if data storage ra:settings cur{type:"int"} if score #n ra.set.tmp > #hi ra.set.tmp run scoreboard players operation #n ra.set.tmp = #hi ra.set.tmp
$execute if data storage ra:settings cur{type:"int"} run scoreboard players operation @s $(obj) = #n ra.set.tmp

# list: step forward and wrap.
execute if data storage ra:settings cur{type:"list"} run scoreboard players add #n ra.set.tmp 1
execute if data storage ra:settings cur{type:"list"} store result score #len ra.set.tmp run data get storage ra:settings cur.values
execute if data storage ra:settings cur{type:"list"} if score #len ra.set.tmp matches 1.. run scoreboard players operation #n ra.set.tmp %= #len ra.set.tmp
$execute if data storage ra:settings cur{type:"list"} run scoreboard players operation @s $(obj) = #n ra.set.tmp
