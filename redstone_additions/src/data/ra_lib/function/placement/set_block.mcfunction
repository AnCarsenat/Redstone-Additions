# /ra_lib:placement/set_block
# Place block with facing from storage. At position. Uses storage ra:temp {block_id, facing_name, has_facing}.

# Place with or without facing depending on has_facing flag
execute if data storage ra:temp {has_facing:1b} run function ra_lib:placement/set_block_facing with storage ra:temp
execute unless data storage ra:temp {has_facing:1b} run function ra_lib:placement/set_block_simple with storage ra:temp
