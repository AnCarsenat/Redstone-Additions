# /ra_ender:blocks/item_vault/pull
# Ask the partner for one stack, because something was taken out of this end.
# Context: as the vault marker, at the barrel.
#
# Rather than a second copy of the move logic with the roles swapped, this makes
# itself the only eligible receiver for one command and runs the partner's push.

tag @s add ra.ender.pull_target

data modify storage ra:ender link set value {}
data modify storage ra:ender link.channel set from entity @s data.properties.channel
data modify storage ra:ender link.recv set value "ra.ender.pull_target"
function ra_ender:link/request_items with storage ra:ender link

tag @s remove ra.ender.pull_target
