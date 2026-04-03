# /ra_interactive:blocks/item_mover/consume_selected_input
# Consume from primary input or connected partner half, based on #mover_input_partner.
# Context: positioned at primary input container

execute if score #mover_input_partner ra.temp matches 0 run function ra_interactive:blocks/item_mover/consume_input

execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/consume_input

execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/consume_input
execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/consume_input

return 1
