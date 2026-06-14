# /ra_multiblock:blast_forge/process
# Manual entry point for blast forge processing (debug / external calls)
# Context: as multiblock marker, at base position
# For normal operation, tick_facing calls process_facing directly

execute if data entity @s data{facing:"north"} run function ra_multiblock:blast_forge/process_facing with storage ra:multiblock bf_dir.north
execute if data entity @s data{facing:"south"} run function ra_multiblock:blast_forge/process_facing with storage ra:multiblock bf_dir.south
execute if data entity @s data{facing:"east"} run function ra_multiblock:blast_forge/process_facing with storage ra:multiblock bf_dir.east
execute if data entity @s data{facing:"west"} run function ra_multiblock:blast_forge/process_facing with storage ra:multiblock bf_dir.west

