# /ra_multiblock:blast_forge/validate
# Validate blast forge structure for assembly — tries all 4 directions
# Uses unified check_blocks macro (single source of truth for block pattern)
# Context: at multiblock base (controller) position
# Sets #mb_result ra.multiblock to 1 if valid in any direction
# Hook: #ra_lib_multiblock:validate

# Only handle blast_forge type
execute unless data storage ra:multiblock {type:"blast_forge"} run return 0

# Try each direction using unified check_blocks — store result captures return value (1=valid, 0=invalid)
execute store result score #mb_result ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.north
execute if score #mb_result ra.multiblock matches 1 run function ra_multiblock:blast_forge/store_assembly_data {dir:"north"}
execute if score #mb_result ra.multiblock matches 1 run return 1

execute store result score #mb_result ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.south
execute if score #mb_result ra.multiblock matches 1 run function ra_multiblock:blast_forge/store_assembly_data {dir:"south"}
execute if score #mb_result ra.multiblock matches 1 run return 1

execute store result score #mb_result ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.east
execute if score #mb_result ra.multiblock matches 1 run function ra_multiblock:blast_forge/store_assembly_data {dir:"east"}
execute if score #mb_result ra.multiblock matches 1 run return 1

execute store result score #mb_result ra.multiblock run function ra_multiblock:blast_forge/check_blocks with storage ra:multiblock bf_dir.west
execute if score #mb_result ra.multiblock matches 1 run function ra_multiblock:blast_forge/store_assembly_data {dir:"west"}
execute if score #mb_result ra.multiblock matches 1 run return 1

