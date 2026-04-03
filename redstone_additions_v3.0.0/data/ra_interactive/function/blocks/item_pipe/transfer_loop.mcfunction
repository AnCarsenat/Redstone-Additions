# /ra_interactive:blocks/item_pipe/transfer_loop
# Repeatedly move one item from source pipe (^ ^ ^-1) into current destination.
# Stops when source slot 0 is empty or destination rejects insert.

# Stop if source slot 0 is empty.
execute positioned ^ ^ ^-1 unless data block ~ ~ ~ Items[0] run return 0

# Refresh current source item.
execute positioned ^ ^ ^-1 run data modify storage ra:temp pipe_item set from block ~ ~ ~ Items[0]
execute unless data storage ra:temp pipe_item.components run data modify storage ra:temp pipe_item.components set value {}

# Attempt to insert exactly one item.
data modify storage ra:temp pipe_item.count set value 1
execute store result score @s ra.temp run function ra_lib:inventory/insert with storage ra:temp pipe_item
execute if score @s ra.temp matches 0 run return 0

# Consume one item from source pipe slot 0.
execute positioned ^ ^ ^-1 store result score @s ra.temp run function ra_lib:inventory/consume_entry0
execute if score @s ra.temp matches 0 run return 0

# Mark success and continue.
tag @s add ra.pipe_moved_any
function ra_interactive:blocks/item_pipe/transfer_loop