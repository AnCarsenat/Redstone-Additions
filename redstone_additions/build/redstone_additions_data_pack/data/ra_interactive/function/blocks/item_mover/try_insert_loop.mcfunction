# /ra_interactive:blocks/item_mover/try_insert_loop
# Insert one item at a time into output/partner output until full or source exhausted.
# Context: positioned at primary output container

execute if score @s ra.inv.count matches ..0 run return 0

# Primary output
execute run function ra_interactive:blocks/item_mover/try_insert_one

# If failed, try connected chest partner
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/try_insert_one

# Trapped chest partner variants
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_interactive:blocks/item_mover/try_insert_one
execute if score @s ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_interactive:blocks/item_mover/try_insert_one

# Stop when output can no longer accept one item.
execute if score @s ra.temp matches 0 run return 0

scoreboard players add #mover_inserted ra.temp 1
scoreboard players remove @s ra.inv.count 1
function ra_interactive:blocks/item_mover/try_insert_loop
