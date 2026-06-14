# /ra_multiblock:upgrade_platform/on_break
# Cleanup when upgrade platform is disassembled
# Context: as multiblock marker, at base position
# Hook: #ra_lib_multiblock:on_break

execute unless entity @s[tag=ra.multiblock.upgrade_platform] run return 0

# No cleanup needed — platform has no containers
