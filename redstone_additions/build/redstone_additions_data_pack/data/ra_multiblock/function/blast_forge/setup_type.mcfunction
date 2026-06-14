# /ra_multiblock:blast_forge/setup_type
# Add type-specific tag to newly created multiblock marker
# Context: as newly summoned marker entity
# Hook: #ra_lib_multiblock:setup_type

execute if data entity @s data{type:"blast_forge"} run tag @s add ra.multiblock.blast_forge
