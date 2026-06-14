# /ra_multiblock:blocks/check_break
# Check if a multiblock base block has been broken and dispatch cleanup
# Context: as multiblock base armor stand, at its position

execute if entity @s[tag=ra.custom_block.multiblock_base.copper] unless block ~ ~ ~ waxed_copper_block run return run function ra_multiblock:blocks/base_on_break with storage ra:multiblock tier_data.copper
execute if entity @s[tag=ra.custom_block.multiblock_base.iron] unless block ~ ~ ~ iron_block run return run function ra_multiblock:blocks/base_on_break with storage ra:multiblock tier_data.iron
execute if entity @s[tag=ra.custom_block.multiblock_base.gold] unless block ~ ~ ~ gold_block run return run function ra_multiblock:blocks/base_on_break with storage ra:multiblock tier_data.gold
execute if entity @s[tag=ra.custom_block.multiblock_base.diamond] unless block ~ ~ ~ diamond_block run return run function ra_multiblock:blocks/base_on_break with storage ra:multiblock tier_data.diamond
execute if entity @s[tag=ra.custom_block.multiblock_base.netherite] unless block ~ ~ ~ netherite_block run return run function ra_multiblock:blocks/base_on_break with storage ra:multiblock tier_data.netherite
