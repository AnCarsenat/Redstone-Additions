# /ra_storage:storage_box/empty_crate_here
# Context: positioned at the container half whose Items[0] is the Item Crate.
# Empties that crate completely into the output named by storage ra:temp unboxer,
# then sends the emptied crate after it.

# The bound is a safety net, not a limit: a crate holds at most one container's
# worth of stacks, and a double chest is 54.
scoreboard players set #unboxer_guard ra.temp 64
function ra_storage:storage_box/empty_crate_loop

# Migrate a touched legacy crate to the modern key.
execute if data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box_item run data modify block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.item_box set value 1b

# Crate is empty now, so pass it on rather than leaving it to block the input.
execute unless data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0] run function ra_storage:storage_box/eject_empty_crate
