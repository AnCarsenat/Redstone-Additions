# /ra:tools/wrench/found_block
# Handle found block for wrench
# Context: as custom block armor stand

data modify storage ra:temp wrench_found set value 1b

# Toggle wireless emitters
execute if entity @s[tag=ra.custom_block.emitter] run function ra_wireless:blocks/emitter/toggle

# Toggle wireless receivers
execute if entity @s[tag=ra.custom_block.receiver] run function ra_wireless:blocks/receiver/toggle

# Multiblock bases - try to assemble or toggle existing multiblock
execute if entity @s[tag=ra.multiblock_base] at @s run function ra:tools/wrench/handle_multiblock_base
execute if entity @s[tag=ra.custom_block.multiblock_base] at @s run function ra:tools/wrench/handle_multiblock_base

# No action for other blocks (excluding multiblock bases)
execute unless entity @s[tag=ra.custom_block.emitter] unless entity @s[tag=ra.custom_block.receiver] unless entity @s[tag=ra.multiblock_base] unless entity @s[tag=ra.custom_block.multiblock_base] run tellraw @a[tag=ra.wrench_active,distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"This block cannot be toggled.",color:"gray"}]
