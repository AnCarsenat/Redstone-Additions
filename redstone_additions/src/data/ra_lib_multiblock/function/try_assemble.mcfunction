# /ra_lib_multiblock:try_assemble
# Try to assemble a multiblock at current position
# Context: at block position (aligned to block center)
# Input storage ra:multiblock: {type:"multiblock_id"}
# Returns: 1 if assembled, 0 if failed

# Reset result
scoreboard players set #mb_result ra.multiblock 0

# Normalize position to block center for consistent distance checks
# Check if already assembled here (marker within same block)
execute align xyz positioned ~0.5 ~ ~0.5 if entity @e[tag=ra.multiblock,distance=..0.5,limit=1,sort=nearest] run return 0

# Dispatch to registered multiblock type validators
# Each validator checks its own type and sets #mb_result to 1 if valid
function #ra_lib_multiblock:validate

# If validation passed, create the multiblock marker
execute if score #mb_result ra.multiblock matches 1 run function ra_lib_multiblock:create_marker

# Clean up assembly staging data from storage
data remove storage ra:multiblock facing
data remove storage ra:multiblock inputs
data remove storage ra:multiblock outputs
data remove storage ra:multiblock controls

return run scoreboard players get #mb_result ra.multiblock
