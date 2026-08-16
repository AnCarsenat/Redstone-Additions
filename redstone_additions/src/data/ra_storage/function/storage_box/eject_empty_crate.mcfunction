# /ra_storage:storage_box/eject_empty_crate
# Context: positioned at the container half holding a now-empty crate at Items[0].
# Refreshes its lore and passes it to the output, so a stack of crates keeps
# feeding through instead of stalling on the one that was just emptied.
#
# It goes to the output container, not onto the floor — insert_or_drop only drops
# it if the output has no room.

data modify storage ra:temp storage_box.target_box set from block ~ ~ ~ Items[0]
function ra_storage:storage_box/update_lore_storage_target

data modify storage ra:temp mover_item set from storage ra:temp storage_box.target_box
data remove storage ra:temp mover_item.Slot
data remove block ~ ~ ~ Items[0]

function ra_storage:storage_box/deliver_stack with storage ra:temp unboxer
