# /ra_multiblock:drill/on_break
# Cleanup when drill is disassembled
# Context: as multiblock marker, at base position
# Hook: #ra_lib_multiblock:on_break

# Only handle drill markers
execute unless entity @s[tag=ra.multiblock.drill] run return 0

# No cleanup needed — items remain in barrels and players can access them directly.
