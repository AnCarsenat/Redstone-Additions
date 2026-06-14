# /ra_multiblock:blast_forge/consume_first_item
# Remove 1 item from the first occupied slot of container at current position
# Context: positioned at a container block
# Returns: 1 if consumed, 0 if empty

# Check container has items
execute unless data block ~ ~ ~ Items[0] run return 0

# Get current count
execute store result score #bf_count ra.temp run data get block ~ ~ ~ Items[0].count

# If only 1 item, remove the slot entirely
execute if score #bf_count ra.temp matches 1 run data remove block ~ ~ ~ Items[0]
execute if score #bf_count ra.temp matches 1 run return 1

# More than 1: decrement count
scoreboard players remove #bf_count ra.temp 1
execute store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get #bf_count ra.temp
