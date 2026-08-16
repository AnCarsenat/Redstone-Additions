# /ra_storage:storage_box/deliver_stack {output:"^ ^ ^1"}
# Internal: put storage ra:temp mover_item into the unboxer's output container.
#
# `at @s` first: the caller is positioned at the input container, and a caret
# offset is measured from the current position, so without this the output offset
# would be applied relative to the input side rather than to the block.

$execute at @s positioned $(output) run function ra_lib:inventory/insert_or_drop with storage ra:temp mover_item
