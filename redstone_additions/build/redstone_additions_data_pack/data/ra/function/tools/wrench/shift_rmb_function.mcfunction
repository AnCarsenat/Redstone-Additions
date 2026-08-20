# /ra:tools/wrench/shift_rmb_function
# Called when shift+right-click with wrench. As player.

# Find nearest custom block
data remove storage ra:temp wrench_found
# The menu and every cycler message this player specifically; the wrench runs
# `as` the marker from here on and would otherwise have no idea who swung it.
tag @s add ra.wrench_user
execute at @s anchored eyes positioned ^ ^ ^0.5 as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^1.5 unless data storage ra:temp wrench_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^2.5 unless data storage ra:temp wrench_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^3.5 unless data storage ra:temp wrench_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^4.5 unless data storage ra:temp wrench_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block

# If no block found
execute unless data storage ra:temp wrench_found run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"No cyclable block found nearby.",color:"gray"}]
data remove storage ra:temp wrench_found

tag @a remove ra.wrench_user
