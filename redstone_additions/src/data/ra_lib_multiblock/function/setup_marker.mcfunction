# /ra_lib_multiblock:setup_marker
# Initialize multiblock marker entity with standardized data structure
# Context: as newly summoned marker entity (has tag ra.multiblock.new)
#
# Standard multiblock data structure:
#   data.type        - multiblock type ID (e.g., "blast_forge")
#   data.facing      - facing direction ("north"/"south"/"east"/"west")
#   data.properties  - {enabled:1b, speed:1}
#   data.inputs      - {input_1:{x,y,z}, ...}  IO block offsets from base
#   data.outputs     - {output_1:{x,y,z}, ...}
#   data.controls    - {redstone_in:{x,y,z}, ...}

# Initialize full data structure atomically via merge
# This guarantees the compound + all keys exist before individual writes
data merge entity @s {data:{type:"unknown",facing:"none",properties:{enabled:1b,speed:1},inputs:{},outputs:{},controls:{}}}

# Populate from assembly storage
data modify entity @s data.type set from storage ra:multiblock type
data modify entity @s data.facing set from storage ra:multiblock facing
execute if data storage ra:multiblock inputs run data modify entity @s data.inputs set from storage ra:multiblock inputs
execute if data storage ra:multiblock outputs run data modify entity @s data.outputs set from storage ra:multiblock outputs
execute if data storage ra:multiblock controls run data modify entity @s data.controls set from storage ra:multiblock controls

# Add type-specific tags via registered hooks
function #ra_lib_multiblock:setup_type

# Grace period — skip first validation cycle to avoid race conditions
tag @s add ra.multiblock.grace

# Remove initialization tag
tag @s remove ra.multiblock.new
