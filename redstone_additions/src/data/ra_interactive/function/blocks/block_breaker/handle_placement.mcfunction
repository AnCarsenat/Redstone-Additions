# Handle block breaker placement
# Context: as bat, at bat position
# Called via function tag ra:register_placement

# Only process if this is a block breaker bat
execute unless entity @s[tag=ra.place.block_breaker] run return 0

# Place the block using the new macro-based library
function ra_lib:placement/place {block_id:"minecraft:dispenser",block_tag:"block_breaker",dir_type:2}

# Remove ra.new tag now that setup is complete
tag @e[tag=ra.custom_block.block_breaker,tag=ra.new] remove ra.new

return 1