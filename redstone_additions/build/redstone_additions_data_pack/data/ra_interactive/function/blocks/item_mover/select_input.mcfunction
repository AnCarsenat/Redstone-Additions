# /ra_interactive:blocks/item_mover/select_input
# Select one input item from current container, or partner half if this is a double chest.
# Context: positioned at primary input container
# Output:
# - #mover_has_input ra.temp = 1 if an item was selected, else 0
# - #mover_input_partner ra.temp = 1 if selected from partner half, else 0
# - storage ra:temp mover_item = selected item entry

scoreboard players set #mover_has_input ra.temp 0
scoreboard players set #mover_input_partner ra.temp 0

# Primary container first
execute if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1

# If empty, probe connected chest partner
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

# Trapped chest variants
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]
execute if score #mover_has_input ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ra.temp 1
execute if score #mover_has_input ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 if data block ~ ~ ~ Items[0] run scoreboard players set #mover_input_partner ra.temp 1

return run scoreboard players get #mover_has_input ra.temp
