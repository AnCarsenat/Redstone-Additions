# /ra:tools/wrench/on_use
# Handle wrench use. As player (from advancement).

# Revoke advancement immediately
advancement revoke @s only ra:tools/wrench_use

# Mark that player clicked this tick (for tag cleanup in tick.mcfunction)
tag @s add ra.wrench_clicked

# If player is already holding (has tag), skip execution
execute if entity @s[tag=ra.wrench_active] run return fail

# Mark player as actively using this tool
tag @s add ra.wrench_active

execute if predicate ra:is_sneaking run return run function ra:tools/wrench/shift_rmb_function
function ra:tools/wrench/rmb_function

# Find nearest custom block via raycast (at @s ensures we're at player position)
# data remove storage ra:temp cdh_found
# execute at @s anchored eyes positioned ^ ^ ^1 as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/creative_data_handler/found_block
# execute at @s anchored eyes positioned ^ ^ ^2 unless data storage ra:temp cdh_found as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/creative_data_handler/found_block
# execute at @s anchored eyes positioned ^ ^ ^3 unless data storage ra:temp cdh_found as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/creative_data_handler/found_block
# execute at @s anchored eyes positioned ^ ^ ^4 unless data storage ra:temp cdh_found as @e[tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/creative_data_handler/found_block

# If no block found
# execute unless data storage ra:temp cdh_found run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"No custom block found nearby.",color:"gray"}]
# data remove storage ra:temp cdh_found
