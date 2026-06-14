# /ra_multiblock:blast_forge/on_break
# Cleanup when blast forge is disassembled
# Context: as multiblock marker, at base position
# Hook: #ra_lib_multiblock:on_break

# Only handle blast forge markers
execute unless entity @s[tag=ra.multiblock.blast_forge] run return 0

# No cleanup needed — items remain in barrels and players can access them directly.
