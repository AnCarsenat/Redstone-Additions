# /ra_ender:blocks/item_vault/shared
# `shared` mode: the channel's contents follow whoever walks up to a vault.
# Context: as the vault marker, at the barrel. Self tag already held.
#
# This is as close to one shared inventory as a data pack can safely get, and the
# reason it is not a straight mirror is duplication. Mirroring the same stacks
# into two barrels gives one stack two extraction points: two players — or two
# hoppers — pulling in the same tick each walk away with it, and by the time any
# function runs the copy already exists. Container clicks cannot be intercepted.
#
# So there is only ever one real copy. It moves to the vault someone is standing
# at, which means the vault you approach is the one holding everything, and the
# far end is empty while you are using this one.

# Someone has to be standing here for the contents to come to this end.
execute unless entity @p[distance=..4] run return 0

function ra_ender:blocks/item_vault/where
data modify storage ra:ender link set value {}
data modify storage ra:ender link.channel set from entity @s data.properties.channel

# Empty: take the lot in one move.
execute unless data block ~ ~ ~ Items[0] run return run function ra_ender:link/claim_shared with storage ra:ender link

# Not empty — someone put something in this end while the far end still held the
# rest. Pulling the whole list over would collide slot numbers, so the two are
# merged a stack per cycle through the same free-slot path the link mode uses.
tag @s add ra.ender.pull_target
function ra_ender:link/request_shared with storage ra:ender link
tag @s remove ra.ender.pull_target
