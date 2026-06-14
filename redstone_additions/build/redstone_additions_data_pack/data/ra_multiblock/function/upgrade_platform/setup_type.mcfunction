# /ra_multiblock:upgrade_platform/setup_type
# Add type-specific tag to newly created multiblock marker
# Context: as newly summoned marker entity
# Hook: #ra_lib_multiblock:setup_type

execute if data entity @s data{type:"upgrade_platform"} run tag @s add ra.multiblock.upgrade_platform
