# /ra_multiblock:blast_forge/check_structure
# Periodic structure integrity check — uses unified check_blocks
# Context: as multiblock marker, at base position
# Sets @s ra.multiblock to 1 if structure is still valid
# Hook: #ra_lib_multiblock:check_structure

# Only handle blast forge markers
execute unless entity @s[tag=ra.multiblock.blast_forge] run return 0

# Debug: verify facing data exists
execute if entity @a[tag=ra.debug,limit=1] unless data entity @s data.facing run tellraw @a[tag=ra.debug] [{text:"[MB Debug] ",color:"gold"},{text:"ERROR: No facing data on marker!",color:"red"},{text:" data="},{nbt:"data",entity:"@s",color:"yellow"}]

# Check structure using unified check_blocks — store result directly sets the score
execute if data entity @s data{facing:"north"} store result score @s ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.north
execute if data entity @s data{facing:"south"} store result score @s ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.south
execute if data entity @s data{facing:"east"} store result score @s ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.east
execute if data entity @s data{facing:"west"} store result score @s ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.west

# Debug: if score still 0 and we have a facing, a block check failed
execute if entity @a[tag=ra.debug,limit=1] if score @s ra.multiblock matches 0 if data entity @s data.facing run tellraw @a[tag=ra.debug] [{text:"[MB Debug] ",color:"gold"},{text:"Structure check FAILED for facing: ",color:"red"},{nbt:"data.facing",entity:"@s",color:"yellow"}]
