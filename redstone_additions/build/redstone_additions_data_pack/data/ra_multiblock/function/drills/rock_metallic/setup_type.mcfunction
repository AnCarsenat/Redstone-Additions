# /ra_multiblock:drill/setup_type
# Add type-specific tag to newly created drill multiblock marker
# Context: as newly summoned marker entity
# Hook: #ra_lib_multiblock:setup_type

execute if data entity @s data{type:"drill"} run tag @s add ra.multiblock.drill
