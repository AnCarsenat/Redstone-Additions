# /ra_storage:storage_box/empty_crate_here
# Context: positioned at the container half whose Items[0] is the Item Crate.
# Empties that crate completely into the output named by storage ra:temp unboxer,
# then destroys the spent crate.

# The bound is a safety net, not a limit: a crate holds at most one container's
# worth of stacks, and a double chest is 54.
scoreboard players set #unboxer_guard ra.temp 64
function ra_storage:storage_box/empty_crate_loop

# The crate is consumed by unboxing. Removing it also clears the way for the next
# crate in the container, which an emptied one left sitting at Items[0] would
# otherwise block.
execute unless data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0] run data remove block ~ ~ ~ Items[0]
