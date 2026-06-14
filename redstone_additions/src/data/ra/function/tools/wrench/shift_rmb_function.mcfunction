# /ra:tools/wrench/shift_rmb_function
# Called when shift+right-click with wrench. As player.

# Find nearest custom block
data remove storage ra:temp wrench_found
execute at @s anchored eyes positioned ^ ^ ^0.5 as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^1.5 unless data storage ra:temp wrench_found as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^2.5 unless data storage ra:temp wrench_found as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^3.5 unless data storage ra:temp wrench_found as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block
execute at @s anchored eyes positioned ^ ^ ^4.5 unless data storage ra:temp wrench_found as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/wrench/cycle_block

# If no block found
execute unless data storage ra:temp wrench_found run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"No cyclable block found nearby.",color:"gray"}]
data remove storage ra:temp wrench_found
