# /ra_multiblock:drill/check_structure
# Periodic structure integrity check — uses check_blocks
# Context: as multiblock marker, at base position
# Sets @s ra.multiblock to 1 if structure is still valid
# Hook: #ra_lib_multiblock:check_structure

# Only handle drill markers
execute unless entity @s[tag=ra.multiblock.drill] run return 0

# Debug: verify facing data exists
execute if entity @a[tag=ra.debug,limit=1] unless data entity @s data.facing run tellraw @a[tag=ra.debug] [{"text":"[Drill Debug] ","color":"gold"},{"text":"ERROR: No facing data on marker!","color":"red"},{"text":" data="},{"nbt":"data","entity":"@s","color":"yellow"}]

# Check structure using check_blocks — store result directly sets the score
execute store result score @s ra.multiblock run function ra_multiblock:drills/rock_metallic/check_blocks

# Debug: if score still 0 and we have a facing, a block check failed
execute if entity @a[tag=ra.debug,limit=1] if score @s ra.multiblock matches 0 if data entity @s data.facing run tellraw @a[tag=ra.debug] [{"text":"[Drill Debug] ","color":"gold"},{"text":"Structure check FAILED for facing: ","color":"red"},{"nbt":"data.facing","entity":"@s","color":"yellow"}]
