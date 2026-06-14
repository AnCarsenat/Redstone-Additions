# /ra:tools/wrench/try_multiblock
# Try to assemble a multiblock at looked-at position
# Context: as player, at position where looking
# Called when no custom block found by wrench

# Fallback path: if marker raycast misses, allow direct block-based interaction

# Toggle existing multiblock marker at targeted position
execute if entity @e[tag=ra.multiblock,distance=..0.5,limit=1,sort=nearest] run return run function ra:tools/wrench/toggle_multiblock

# Try to assemble based on looked-at base block tier
execute if block ~ ~ ~ minecraft:waxed_copper_block run return run function ra:tools/wrench/try_assemble_copper
execute if block ~ ~ ~ minecraft:iron_block run return run function ra:tools/wrench/try_assemble_iron
execute if block ~ ~ ~ minecraft:gold_block run return run function ra:tools/wrench/try_assemble_gold
execute if block ~ ~ ~ minecraft:diamond_block run return run function ra:tools/wrench/try_assemble_diamond
execute if block ~ ~ ~ minecraft:netherite_block run return run function ra:tools/wrench/try_assemble_netherite
