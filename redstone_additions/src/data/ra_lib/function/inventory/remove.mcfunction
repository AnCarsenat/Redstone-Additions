# /ra_lib:inventory/remove {id:"minecraft:...",count:N}
# Remove items from container at current position. Macro function.
# Input: $(id) = item ID (e.g., "minecraft:wheat"), $(count) = quantity to remove
# Output: returns 1 if removed, 0 if item not found or insufficient quantity
#
# Example: function ra_lib:inventory/remove {id:"minecraft:wheat",count:1}

# Check if item exists in container
$execute unless data block ~ ~ ~ Items[{id:"$(id)"}] run return 0

# Copy the matching item to storage
$data modify storage ra:inventory remove_item set from block ~ ~ ~ Items[{id:"$(id)"}]

# Get current count and slot
execute store result score #count ra.temp run data get storage ra:inventory remove_item.count
execute store result score #slot ra.temp run data get storage ra:inventory remove_item.Slot

# Get amount to remove
$scoreboard players set #to_remove ra.temp $(count)

# Check if we have enough
execute if score #count ra.temp < #to_remove ra.temp run return 0

# Calculate new count
scoreboard players operation #count ra.temp -= #to_remove ra.temp

# Update or remove based on slot (supports slots 0-8 for dispensers)
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 0 store result block ~ ~ ~ Items[{Slot:0b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 1 store result block ~ ~ ~ Items[{Slot:1b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 2 store result block ~ ~ ~ Items[{Slot:2b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 3 store result block ~ ~ ~ Items[{Slot:3b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 4 store result block ~ ~ ~ Items[{Slot:4b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 5 store result block ~ ~ ~ Items[{Slot:5b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 6 store result block ~ ~ ~ Items[{Slot:6b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 7 store result block ~ ~ ~ Items[{Slot:7b}].count int 1 run scoreboard players get #count ra.temp
execute if score #count ra.temp matches 1.. if score #slot ra.temp matches 8 store result block ~ ~ ~ Items[{Slot:8b}].count int 1 run scoreboard players get #count ra.temp

execute if score #count ra.temp matches 0 if score #slot ra.temp matches 0 run data remove block ~ ~ ~ Items[{Slot:0b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 1 run data remove block ~ ~ ~ Items[{Slot:1b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 2 run data remove block ~ ~ ~ Items[{Slot:2b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 3 run data remove block ~ ~ ~ Items[{Slot:3b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 4 run data remove block ~ ~ ~ Items[{Slot:4b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 5 run data remove block ~ ~ ~ Items[{Slot:5b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 6 run data remove block ~ ~ ~ Items[{Slot:6b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 7 run data remove block ~ ~ ~ Items[{Slot:7b}]
execute if score #count ra.temp matches 0 if score #slot ra.temp matches 8 run data remove block ~ ~ ~ Items[{Slot:8b}]

return 1
