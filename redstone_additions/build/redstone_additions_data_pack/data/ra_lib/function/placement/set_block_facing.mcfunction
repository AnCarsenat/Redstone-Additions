# /ra_lib:placement/set_block_facing
# Place block WITH facing property. Macro function.
# Uses storage ra:temp {block_id, facing_name}

$setblock ~ ~ ~ $(block_id)[facing=$(facing_name)] replace
