# /ra_wireless:tools/remote/apply_to_slot {slot:N}
# Internal: stamp the channel onto the remote in hotbar slot N.
# Returns 1 when applied, 0 when that slot no longer holds a remote.
#
# The slot is checked rather than trusted: the player had a whole book-writing
# session in which to move the item, and stamping the wrong slot would rewrite
# whatever else ended up there.

# Matched on the custom_data alone. Adding max_stack_size here would narrow the
# test to remotes made since they became unstackable, and one from an older world
# would silently fail to take a channel.
$execute unless data entity @s Inventory[{Slot:$(slot)b,components:{"minecraft:custom_data":{ra:{remote:1b}}}}] run return 0

$item modify entity @s hotbar.$(slot) ra_wireless:set_channel
return 1
