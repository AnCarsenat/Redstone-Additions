# /ra_multiblock:blast_forge/tick
# Per-tick processing for blast forge
# Context: as multiblock marker, at base position
# Dispatches to tick_facing macro with direction-specific offsets

# Skip if disabled
execute unless data entity @s data.properties{enabled:1b} run return 0

# Dispatch direction-specific tick logic via macro
execute if data entity @s data{facing:"north"} run function ra_multiblock:blast_forge/tick_facing with storage ra:multiblock bf_dir.north
execute if data entity @s data{facing:"south"} run function ra_multiblock:blast_forge/tick_facing with storage ra:multiblock bf_dir.south
execute if data entity @s data{facing:"east"} run function ra_multiblock:blast_forge/tick_facing with storage ra:multiblock bf_dir.east
execute if data entity @s data{facing:"west"} run function ra_multiblock:blast_forge/tick_facing with storage ra:multiblock bf_dir.west
